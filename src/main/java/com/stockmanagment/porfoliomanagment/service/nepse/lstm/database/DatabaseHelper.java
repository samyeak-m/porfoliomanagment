package com.stockmanagment.porfoliomanagment.service.nepse.lstm.database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.PropertyLoader;

@Service
public class DatabaseHelper {
    private static final Logger LOGGER = Logger.getLogger(DatabaseHelper.class.getName());
    
    // ADD: Cache for stock symbols with TTL
    private volatile List<String> cachedStockSymbols = null;
    private volatile long lastCacheUpdate = 0;
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5 minutes

    private final String url;
    private final String username;
    private final String password;
    private Map<String, Double> tableNameMap;

    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_POOL_SIZE = 2;

    public DatabaseHelper() {
        Properties properties = PropertyLoader.loadProperties("application.properties");
        this.url = properties.getProperty("spring.datasource.nepse.jdbc-url");
        this.username = properties.getProperty("spring.datasource.nepse.username");
        this.password = properties.getProperty("spring.datasource.nepse.password");
        this.tableNameMap = new HashMap<>();

        try {
            generateTableNameMap();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error generating table name map during initialization", e);
        }
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private Connection getPooledConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private void generateTableNameMap() throws SQLException {
        this.tableNameMap = new HashMap<>();
        List<String> tableNames = getAllStockTableNames();

        if (tableNames.isEmpty()) {
            LOGGER.log(Level.WARNING, "No stock tables found in database");
            return;
        }

        double step = tableNames.size() > 1 ? 1.0 / (tableNames.size() - 1) : 0.5;

        for (int i = 0; i < tableNames.size(); i++) {
            double normalizedValue = tableNames.size() == 1 ? 0.5 : i * step;
            tableNameMap.put(tableNames.get(i), normalizedValue);
        }
    }

    // OPTIMIZED: Get all stock symbols with caching and batch validation
    public List<String> getAllStockTableNames() throws SQLException {
        // Check cache first
        if (cachedStockSymbols != null && 
            (System.currentTimeMillis() - lastCacheUpdate) < CACHE_TTL) {
            return new ArrayList<>(cachedStockSymbols);
        }
        
        List<String> tableNames = new ArrayList<>();
        
        // OPTIMIZED: Use single query with batch validation
        String batchQuery = """
            SELECT table_name, 
                   (SELECT COUNT(*) FROM information_schema.tables t2 
                    WHERE t2.table_name = t1.table_name 
                    AND EXISTS (
                        SELECT 1 FROM information_schema.columns 
                        WHERE table_name = t1.table_name 
                        AND column_name = 'close'
                    )) as has_close_column
            FROM information_schema.tables t1
            WHERE table_schema = 'nepse' 
            AND table_name LIKE 'daily_data_%'
            ORDER BY table_name
        """;

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(batchQuery);
             ResultSet rs = pstmt.executeQuery()) {
            
            // Collect all table names first
            List<String> candidateTables = new ArrayList<>();
            while (rs.next()) {
                String tableName = rs.getString("table_name")
                    .replace("daily_data_", "").toLowerCase();
                candidateTables.add(tableName);
            }
            
            // OPTIMIZED: Batch validate close prices with single query
            if (!candidateTables.isEmpty()) {
                tableNames = batchValidateClosePrices(candidateTables);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching stock table names", e);
            throw e;
        }
        
        // Update cache
        cachedStockSymbols = new ArrayList<>(tableNames);
        lastCacheUpdate = System.currentTimeMillis();
        
        LOGGER.log(Level.INFO, "Loaded " + tableNames.size() + " valid stock symbols");
        return tableNames;
    }

    // NEW: Batch validate close prices in single query
    private List<String> batchValidateClosePrices(List<String> candidateTables) throws SQLException {
        List<String> validTables = new ArrayList<>();
        
        // Build dynamic UNION query for batch validation
        StringBuilder unionQuery = new StringBuilder();
        for (int i = 0; i < candidateTables.size(); i++) {
            if (i > 0) unionQuery.append(" UNION ALL ");
            
            String tableName = candidateTables.get(i);
            unionQuery.append(String.format(
                "(SELECT '%s' as table_name, COUNT(*) as valid_count " +
                "FROM daily_data_%s WHERE close >= 100 LIMIT 1)", 
                tableName, tableName
            ));
        }
        
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(unionQuery.toString());
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                String tableName = rs.getString("table_name");
                int validCount = rs.getInt("valid_count");
                
                if (validCount > 0) {
                    validTables.add(tableName);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Batch validation failed, falling back to individual checks", e);
            // Fallback to individual validation
            return validateIndividually(candidateTables);
        }
        
        return validTables;
    }
    
    // FALLBACK: Individual validation if batch fails
    private List<String> validateIndividually(List<String> candidateTables) throws SQLException {
        List<String> validTables = new ArrayList<>();
        
        String query = "SELECT COUNT(*) FROM daily_data_? WHERE close >= 100 LIMIT 1";
        
        try (Connection conn = connect()) {
            for (String tableName : candidateTables) {
                String specificQuery = query.replace("?", tableName);
                try (PreparedStatement pstmt = conn.prepareStatement(specificQuery);
                     ResultSet rs = pstmt.executeQuery()) {
                    
                    if (rs.next() && rs.getInt(1) > 0) {
                        validTables.add(tableName);
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error validating table: " + tableName, e);
                }
            }
        }
        
        return validTables;
    }
    
    // NEW: Method to clear cache when needed
    public void clearCache() {
        cachedStockSymbols = null;
        lastCacheUpdate = 0;
        if (tableNameMap != null) {
            tableNameMap.clear();
        }
    }
    
    // OPTIMIZED: Async cache warming
    @Async
    public void warmCache() {
        try {
            getAllStockTableNames();
            LOGGER.log(Level.INFO, "Stock symbols cache warmed successfully");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to warm stock symbols cache", e);
        }
    }

    private void createPredictionsTableIfNotExists() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS predictions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "stock_symbol VARCHAR(10) NOT NULL, " +
                "point_change DOUBLE NOT NULL, " +
                "price_change DOUBLE NOT NULL, " +
                "prediction DOUBLE NOT NULL, " +
                "lastclose DOUBLE NOT NULL, " +
                "prediction_date DATE NOT NULL DEFAULT CURRENT_DATE" +
                ")";

        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableSQL);

            try {
                stmt.executeUpdate(
                        "ALTER TABLE predictions ADD COLUMN IF NOT EXISTS stock_symbol VARCHAR(10) NOT NULL");
            } catch (SQLException e) {
            }

            System.out.println("Table Created/Updated");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating predictions table", e);
            throw e;
        }
    }

    public void savePredictions(String stockSymbol, double[] predictions, double[] lastclose) throws SQLException {
        createPredictionsTableIfNotExists();

        System.out.println("prediction : " + Arrays.toString(predictions) + ", Last close : "
                + Arrays.toString(lastclose) + ", symbol : " + stockSymbol);

        String query = "INSERT INTO predictions (stock_symbol, prediction, lastclose, point_change, price_change, prediction_date) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            conn.setAutoCommit(false);
            LocalDate predictionDate = LocalDate.now();

            for (int i = 0; i < predictions.length; i++) {
                double prediction = predictions[i];
                double close = lastclose[i];
                double pointChange = prediction - close;
                double priceChange = (pointChange / close) * 100;

                pstmt.setString(1, stockSymbol);
                pstmt.setDouble(2, prediction);
                pstmt.setDouble(3, close);
                pstmt.setDouble(4, pointChange);
                pstmt.setDouble(5, priceChange);
                pstmt.setDate(6, Date.valueOf(predictionDate));
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            conn.commit();
            System.out.println("Saved predictions for stock symbol: " + stockSymbol);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving predictions for stock symbol: " + stockSymbol, e);
            throw e;
        }
    }

    public void debugTableNameMap() {
        System.out.println("=== TABLE NAME MAP DEBUG ===");
        System.out.println("Map size: " + (tableNameMap != null ? tableNameMap.size() : "null"));

        if (tableNameMap != null && !tableNameMap.isEmpty()) {
            System.out.println("Map contents:");
            for (Map.Entry<String, Double> entry : tableNameMap.entrySet()) {
                System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
            }
        } else {
            System.out.println("Map is empty or null!");
            try {
                List<String> tables = getAllStockTableNames();
                System.out.println("Available tables: " + tables);
            } catch (SQLException e) {
                System.out.println("Error getting table names: " + e.getMessage());
            }
        }
        System.out.println("=== END DEBUG ===");
    }

    // ADD: Missing loadStockData method
    public List<double[]> loadStockData(String stockSymbol) throws SQLException {
        List<double[]> stockData = new ArrayList<>();
        String tableName = "daily_data_" + stockSymbol.toLowerCase();
        
        // FIX: Remove 'volume' from query
        String query = "SELECT date, close, high, low, open FROM " + tableName + 
                      " WHERE close >= 100 ORDER BY date ASC";
        
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                double[] row = new double[5];
                row[0] = rs.getDate("date").getTime(); // Date as timestamp
                row[1] = rs.getDouble("close");        // Close price
                row[2] = rs.getDouble("high");         // High price
                row[3] = rs.getDouble("low");          // Low price
                row[4] = rs.getDouble("open");         // Open price
                
                // Validate data
                boolean isValid = true;
                for (int i = 1; i < row.length; i++) {
                    if (Double.isNaN(row[i]) || Double.isInfinite(row[i]) || row[i] <= 0) {
                        isValid = false;
                        break;
                    }
                }
                
                if (isValid) {
                    stockData.add(row);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading stock data for: " + stockSymbol, e);
            throw e;
        }
        
        LOGGER.log(Level.INFO, "Loaded " + stockData.size() + " records for " + stockSymbol);
        return stockData;
    }

    // ADD: Missing loadStockDataAfterDate method
    public List<double[]> loadStockDataAfterDate(String stockSymbol, LocalDate afterDate) throws SQLException {
        List<double[]> stockData = new ArrayList<>();
        String tableName = "daily_data_" + stockSymbol.toLowerCase();
        
        String query = "SELECT date, close, high, low, volume, open FROM " + tableName + 
                      " WHERE close >= 100 AND date > ? ORDER BY date ASC";
        
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setDate(1, Date.valueOf(afterDate));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double[] row = new double[6];
                    row[0] = rs.getDate("date").getTime(); // Date as timestamp
                    row[1] = rs.getDouble("close");        // Close price
                    row[2] = rs.getDouble("high");         // High price
                    row[3] = rs.getDouble("low");          // Low price
                    row[4] = rs.getDouble("volume");       // Volume
                    row[5] = rs.getDouble("open");         // Open price
                    
                    // Validate data
                    boolean isValid = true;
                    for (int i = 1; i < row.length; i++) {
                        if (Double.isNaN(row[i]) || Double.isInfinite(row[i]) || row[i] <= 0) {
                            isValid = false;
                            break;
                        }
                    }
                    
                    if (isValid) {
                        stockData.add(row);
                    }
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading stock data after date for: " + stockSymbol, e);
            throw e;
        }
        
        LOGGER.log(Level.INFO, "Loaded " + stockData.size() + " records for " + stockSymbol + " after " + afterDate);
        return stockData;
    }

    // Run this SQL script to add indexes to all daily_data tables
    // SELECT CONCAT('CREATE INDEX idx_close ON ', table_name, ' (close);') as create_index_sql
    // FROM information_schema.tables
    // WHERE table_schema = 'nepse'
    // AND table_name LIKE 'daily_data_%';

    // Example output will be:
    // CREATE INDEX idx_close ON daily_data_ntc (close);
    // CREATE INDEX idx_close ON daily_data_adbl (close);
    // etc.

    // Also add index on date column for better performance
    // SELECT CONCAT('CREATE INDEX idx_date ON ', table_name, ' (date);') as create_index_sql
    // FROM information_schema.tables
    // WHERE table_schema = 'nepse'
    // AND table_name LIKE 'daily_data_%';
}
