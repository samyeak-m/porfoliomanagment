package com.stockmanagment.porfoliomanagment.prediction.database;

import com.stockmanagment.porfoliomanagment.prediction.util.PropertyLoader;

import java.sql.Date;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseHelper {
    private static final Logger LOGGER = Logger.getLogger(DatabaseHelper.class.getName());
    private final String url;
    private final String username;
    private final String password;
    private Map<String, Double> tableNameMap;

    public DatabaseHelper() {
        Properties properties = PropertyLoader.loadProperties("application.properties");
        this.url = properties.getProperty("db.url");
        this.username = properties.getProperty("db.username");
        this.password = properties.getProperty("db.password");
        this.tableNameMap = new HashMap<>();
        try {
            generateTableNameMap();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error generating table name map", e);
        }
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private void generateTableNameMap() throws SQLException {
        List<String> tableNames = getAllStockTableNames();
        double step = 1.0 / (tableNames.size() - 1);

        for (int i = 0; i < tableNames.size(); i++) {
            tableNameMap.put(tableNames.get(i), i * step);
        }
    }

    public List<String> getAllStockTableNames() throws SQLException {
        List<String> tableNames = new ArrayList<>();
        String query = "SHOW TABLES LIKE 'daily_data_%'";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String tableName = rs.getString(1).replace("daily_data_", "");
                tableNames.add(tableName);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching stock table names", e);
            throw e;
        }
        return tableNames;
    }

    public List<double[]> loadStockData(String tableName) throws SQLException {
        List<double[]> stockData = new ArrayList<>();
        String query = "SELECT date, close, high, low, open, volume, turnover FROM daily_data_" + tableName + " WHERE close >= 100 ORDER BY date";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Date date = rs.getDate("date");
                double close = rs.getDouble("close");
                double high = rs.getDouble("high");
                double low = rs.getDouble("low");
                double open = rs.getDouble("open");
                double volume = rs.getDouble("volume");
                double turnover = rs.getDouble("turnover");

                double dateAsDouble = date.getTime();

                // Use the normalized value from the map
                double normalizedTableName = tableNameMap.get(tableName);

                stockData.add(new double[]{normalizedTableName, close, high, low, open, dateAsDouble});
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading stock data for table " + tableName, e);
            throw e;
        }
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
            System.out.println("Table Created");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating predictions table", e);
            throw e;
        }
    }

    public void savePredictions(String stockSymbol, double[] predictions, double[] lastclose) throws SQLException {
        createPredictionsTableIfNotExists();

        System.out.println("prediction : " + Arrays.toString(predictions) + ", Last close : " + Arrays.toString(lastclose) + ", symbol : " + stockSymbol);

        String query = "INSERT INTO predictions (stock_symbol, prediction, lastclose, point_change, price_change, prediction_date) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            conn.setAutoCommit(false);
            LocalDate predictionDate = LocalDate.now();

            for (int i = 0; i < predictions.length; i++) {
                double prediction = predictions[i];
                double close = lastclose[i];
                double pointChange = prediction - close;
                double priceChange = pointChange / close;

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
}
