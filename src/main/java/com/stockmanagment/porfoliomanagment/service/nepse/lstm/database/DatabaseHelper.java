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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.PropertyLoader;

@Service
public class DatabaseHelper {
    private static final Logger LOGGER = Logger.getLogger(DatabaseHelper.class.getName());
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

    public List<String> getAllStockTableNames() throws SQLException {
        List<String> tableNames = new ArrayList<>();
        String query = "SHOW TABLES LIKE 'daily_data_%'";

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                // FIXED: Store table names in lowercase
                String tableName = rs.getString(1).replace("daily_data_", "").toLowerCase();
                if (hasValidClosePrice(tableName)) {
                    tableNames.add(tableName);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching stock table names", e);
            throw e;
        }
        return tableNames;
    }

    private boolean hasValidClosePrice(String tableName) throws SQLException {
        String normalizedTableName = tableName.toLowerCase();
        String query = "SELECT COUNT(*) FROM daily_data_" + normalizedTableName + " WHERE close >= 100";

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking close price for table " + normalizedTableName, e);
            throw e;
        }
        return false;
    }

    public List<double[]> loadStockData(String tableName) throws SQLException {
        List<double[]> stockData = new ArrayList<>();

        String normalizedTableName = tableName.toLowerCase();
        String query = "SELECT date, close, high, low, open FROM daily_data_" + normalizedTableName + " ORDER BY date";

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Date date = rs.getDate("date");
                double close = rs.getDouble("close");
                double high = rs.getDouble("high");
                double low = rs.getDouble("low");
                double open = rs.getDouble("open");

                double dateAsDouble = date.getTime();

                Double normalizedValue = tableNameMap.get(normalizedTableName);
                double normalizedTableNameValue;

                if (normalizedValue == null) {
                    LOGGER.log(Level.WARNING,
                            "Table name '" + normalizedTableName + "' not found in tableNameMap. Regenerating map...");
                    try {
                        generateTableNameMap();
                        normalizedValue = tableNameMap.get(normalizedTableName);
                        if (normalizedValue == null) {
                            // Still null, use default value based on hash
                            normalizedTableNameValue = Math.abs(normalizedTableName.hashCode() % 1000) / 1000.0;
                        } else {
                            normalizedTableNameValue = normalizedValue;
                        }
                    } catch (SQLException e) {
                        normalizedTableNameValue = Math.abs(normalizedTableName.hashCode() % 1000) / 1000.0;
                        LOGGER.log(Level.WARNING, "Failed to regenerate map, using hash fallback: "
                                + normalizedTableNameValue + " for table: " + normalizedTableName);
                    }
                } else {
                    normalizedTableNameValue = normalizedValue;
                }

                stockData.add(new double[] { normalizedTableNameValue, close, high, low, open, dateAsDouble });
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading stock data for table " + normalizedTableName, e);
            throw e;
        }
        return stockData;
    }

    public List<double[]> loadStockDataAfterDate(String tableName, LocalDate afterDate) throws SQLException {
        List<double[]> stockData = new ArrayList<>();

        String normalizedTableName = tableName.toLowerCase();
        String query = "SELECT date, close, high, low, open FROM daily_data_" + normalizedTableName +
                " WHERE date > ? ORDER BY date";

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDate(1, Date.valueOf(afterDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Date date = rs.getDate("date");
                    double close = rs.getDouble("close");
                    double high = rs.getDouble("high");
                    double low = rs.getDouble("low");
                    double open = rs.getDouble("open");

                    double dateAsDouble = date.getTime();

                    Double normalizedValue = tableNameMap.get(normalizedTableName);
                    double normalizedTableNameValue;

                    if (normalizedValue == null) {
                        LOGGER.log(Level.WARNING,
                                "Table name '" + normalizedTableName + "' not found in tableNameMap for date query");
                        try {
                            generateTableNameMap();
                            normalizedValue = tableNameMap.get(normalizedTableName);
                            if (normalizedValue == null) {
                                normalizedTableNameValue = Math.abs(normalizedTableName.hashCode() % 1000) / 1000.0;
                            } else {
                                normalizedTableNameValue = normalizedValue;
                            }
                        } catch (SQLException e) {
                            normalizedTableNameValue = Math.abs(normalizedTableName.hashCode() % 1000) / 1000.0;
                        }
                    } else {
                        normalizedTableNameValue = normalizedValue;
                    }

                    stockData.add(new double[] { normalizedTableNameValue, close, high, low, open, dateAsDouble });
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading stock data after date for table " + normalizedTableName, e);
            throw e;
        }
        return stockData;
    }

    // Load the last N rows (ascending by date) for a symbol's table
    public List<double[]> loadLastNStockData(String tableName, int n) throws SQLException {
        List<double[]> stockData = new ArrayList<>();
        String normalizedTableName = tableName.toLowerCase();
        String query = "SELECT date, close, high, low, open FROM daily_data_" + normalizedTableName + " ORDER BY date DESC LIMIT ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, Math.max(2, n));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Date date = rs.getDate("date");
                    double close = rs.getDouble("close");
                    double high = rs.getDouble("high");
                    double low = rs.getDouble("low");
                    double open = rs.getDouble("open");

                    double dateAsDouble = date.getTime();

                    Double normalizedValue = tableNameMap.get(normalizedTableName);
                    double normalizedTableNameValue;
                    if (normalizedValue == null) {
                        try {
                            generateTableNameMap();
                            normalizedValue = tableNameMap.get(normalizedTableName);
                        } catch (SQLException e) {
                            // ignore and fall back
                        }
                    }
                    normalizedTableNameValue = (normalizedValue != null)
                            ? normalizedValue
                            : Math.abs(normalizedTableName.hashCode() % 1000) / 1000.0;

                    stockData.add(new double[] { normalizedTableNameValue, close, high, low, open, dateAsDouble });
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading last N rows for " + normalizedTableName, e);
            throw e;
        }

        // reverse to ascending by date
        Collections.reverse(stockData);
        return stockData;
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
}