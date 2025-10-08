package com.stockmanagment.porfoliomanagment.service.nepse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class TradingChartService {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/porfoliomanagment_nepse";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    public List<String> getAvailableSymbols() {
        List<String> symbols = new ArrayList<>();
        
        String sql = "SELECT table_name FROM information_schema.tables " +
                     "WHERE table_schema = 'porfoliomanagment_nepse' " +
                     "AND table_name LIKE 'daily_data_%' " +
                     "ORDER BY table_name";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                String tableName = rs.getString("table_name");
                String symbol = tableName.replace("daily_data_", "").toUpperCase();
                
                if (symbol != null && !symbol.isBlank()) {
                    symbols.add(symbol);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading symbols: " + e.getMessage());
            e.printStackTrace();
        }
        
        return symbols;
    }

    public Map<String, Object> getChartDataForSymbol(String symbol) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> dataList = new ArrayList<>();
        
        String normalizedSymbol = symbol.toLowerCase().replace("/", "_");
        String tableName = "daily_data_" + normalizedSymbol;
        
        String sql = "SELECT date, open, high, low, close " +
                     "FROM " + tableName + " " +
                     "ORDER BY date ASC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> dataPoint = new HashMap<>();
                    dataPoint.put("date", rs.getDate("date").toString());
                    dataPoint.put("open", rs.getDouble("open"));
                    dataPoint.put("high", rs.getDouble("high"));
                    dataPoint.put("low", rs.getDouble("low"));
                    dataPoint.put("close", rs.getDouble("close"));
                    dataPoint.put("volume", 0.0);
                    dataList.add(dataPoint);
                }
            }
            
            result.put("symbol", symbol.toUpperCase());
            result.put("data", dataList);
            result.put("indicators", calculateIndicators(dataList));
            
            System.out.println("Loaded " + dataList.size() + " data points for " + symbol + " from table: " + tableName);
            
        } catch (Exception e) {
            System.err.println("Error loading chart data for " + symbol + " from table " + tableName + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    private Map<String, Object> calculateIndicators(List<Map<String, Object>> dataList) {
        Map<String, Object> indicators = new HashMap<>();
        
        int dataSize = dataList.size();
        
        if (dataSize < 2) {
            indicators.put("sma20", new ArrayList<>());
            indicators.put("sma50", new ArrayList<>());
            indicators.put("ema12", new ArrayList<>());
            indicators.put("ema26", new ArrayList<>());
            indicators.put("rsi", new ArrayList<>());
            indicators.put("macd", createEmptyMACD());
            indicators.put("bollingerBands", createEmptyBollingerBands());
            return indicators;
        }

        double[] closePrices = dataList.stream()
                .mapToDouble(d -> (Double) d.get("close"))
                .toArray();

        // FIXED: Use adaptive periods based on available data
        int sma20Period = Math.min(20, Math.max(2, dataSize / 2));
        int sma50Period = Math.min(50, Math.max(2, dataSize));
        int ema12Period = Math.min(12, Math.max(2, dataSize / 3));
        int ema26Period = Math.min(26, Math.max(2, dataSize / 2));
        int rsiPeriod = Math.min(14, Math.max(2, dataSize / 2));
        int bbPeriod = Math.min(20, Math.max(2, dataSize / 2));

        indicators.put("sma20", calculateSMA(closePrices, sma20Period));
        indicators.put("sma50", calculateSMA(closePrices, sma50Period));
        indicators.put("ema12", calculateEMA(closePrices, ema12Period));
        indicators.put("ema26", calculateEMA(closePrices, ema26Period));
        indicators.put("rsi", calculateRSI(closePrices, rsiPeriod));
        indicators.put("macd", calculateMACD(closePrices, ema12Period, ema26Period, Math.min(9, dataSize / 3)));
        indicators.put("bollingerBands", calculateBollingerBands(closePrices, bbPeriod, 2.0));

        return indicators;
    }

    private Map<String, List<Double>> createEmptyMACD() {
        Map<String, List<Double>> macd = new HashMap<>();
        macd.put("macd", new ArrayList<>());
        macd.put("signal", new ArrayList<>());
        macd.put("histogram", new ArrayList<>());
        return macd;
    }

    private Map<String, List<Double>> createEmptyBollingerBands() {
        Map<String, List<Double>> bands = new HashMap<>();
        bands.put("upper", new ArrayList<>());
        bands.put("middle", new ArrayList<>());
        bands.put("lower", new ArrayList<>());
        return bands;
    }

    // FIXED: Improved SMA calculation with better handling of insufficient data
    private List<Double> calculateSMA(double[] prices, int period) {
        List<Double> sma = new ArrayList<>();
        
        // For very small datasets, use expanding window
        if (prices.length < period) {
            for (int i = 0; i < prices.length; i++) {
                double sum = 0;
                int count = Math.min(i + 1, prices.length);
                for (int j = 0; j < count; j++) {
                    sum += prices[i - j];
                }
                sma.add(sum / count);
            }
            return sma;
        }
        
        // Normal SMA calculation
        for (int i = 0; i < prices.length; i++) {
            if (i < period - 1) {
                // Use expanding window for early values
                double sum = 0;
                for (int j = 0; j <= i; j++) {
                    sum += prices[j];
                }
                sma.add(sum / (i + 1));
            } else {
                double sum = 0;
                for (int j = 0; j < period; j++) {
                    sum += prices[i - j];
                }
                sma.add(sum / period);
            }
        }
        return sma;
    }

    // FIXED: Improved EMA calculation
    private List<Double> calculateEMA(double[] prices, int period) {
        List<Double> ema = new ArrayList<>();
        
        if (prices.length == 0) return ema;
        
        double multiplier = 2.0 / (period + 1);
        
        // Start with first price
        ema.add(prices[0]);
        
        // Calculate EMA for subsequent values
        for (int i = 1; i < prices.length; i++) {
            double value = (prices[i] - ema.get(i - 1)) * multiplier + ema.get(i - 1);
            ema.add(value);
        }
        
        return ema;
    }

    // FIXED: Improved RSI calculation with better handling of edge cases
    private List<Double> calculateRSI(double[] prices, int period) {
        List<Double> rsi = new ArrayList<>();
        
        if (prices.length < 2) {
            for (int i = 0; i < prices.length; i++) {
                rsi.add(50.0); // Neutral RSI
            }
            return rsi;
        }
        
        double[] gains = new double[prices.length];
        double[] losses = new double[prices.length];

        // Calculate gains and losses
        for (int i = 1; i < prices.length; i++) {
            double change = prices[i] - prices[i - 1];
            gains[i] = change > 0 ? change : 0;
            losses[i] = change < 0 ? -change : 0;
        }

        // Fill initial values with neutral RSI
        for (int i = 0; i < Math.min(period, prices.length); i++) {
            rsi.add(50.0);
        }

        if (prices.length <= period) {
            return rsi;
        }

        // Calculate initial average gain and loss
        double avgGain = 0;
        double avgLoss = 0;
        for (int i = 1; i <= period; i++) {
            avgGain += gains[i];
            avgLoss += losses[i];
        }
        avgGain /= period;
        avgLoss /= period;

        // Calculate RSI for remaining values
        for (int i = period; i < prices.length; i++) {
            avgGain = ((avgGain * (period - 1)) + gains[i]) / period;
            avgLoss = ((avgLoss * (period - 1)) + losses[i]) / period;
            
            double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
            double rsiValue = 100 - (100 / (1 + rs));
            
            // Ensure RSI is within valid range
            rsiValue = Math.max(0, Math.min(100, rsiValue));
            rsi.add(rsiValue);
        }

        return rsi;
    }

    // FIXED: Improved MACD calculation
    private Map<String, List<Double>> calculateMACD(double[] prices, int shortPeriod, int longPeriod, int signalPeriod) {
        List<Double> emaShort = calculateEMA(prices, shortPeriod);
        List<Double> emaLong = calculateEMA(prices, longPeriod);
        
        List<Double> macdLine = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            macdLine.add(emaShort.get(i) - emaLong.get(i));
        }
        
        // Calculate signal line (EMA of MACD)
        double[] macdArray = macdLine.stream().mapToDouble(Double::doubleValue).toArray();
        List<Double> signalLine = calculateEMA(macdArray, signalPeriod);
        
        // Calculate histogram
        List<Double> histogram = new ArrayList<>();
        for (int i = 0; i < macdLine.size(); i++) {
            histogram.add(macdLine.get(i) - signalLine.get(i));
        }

        Map<String, List<Double>> macd = new HashMap<>();
        macd.put("macd", macdLine);
        macd.put("signal", signalLine);
        macd.put("histogram", histogram);
        
        return macd;
    }

    // FIXED: Improved Bollinger Bands calculation
    private Map<String, List<Double>> calculateBollingerBands(double[] prices, int period, double stdDevMultiplier) {
        List<Double> sma = calculateSMA(prices, period);
        List<Double> upper = new ArrayList<>();
        List<Double> lower = new ArrayList<>();

        for (int i = 0; i < prices.length; i++) {
            if (i < period - 1) {
                // Use expanding window for early values
                double sum = 0;
                int count = i + 1;
                for (int j = 0; j < count; j++) {
                    sum += Math.pow(prices[j] - sma.get(i), 2);
                }
                double stdDev = Math.sqrt(sum / count);
                upper.add(sma.get(i) + (stdDevMultiplier * stdDev));
                lower.add(sma.get(i) - (stdDevMultiplier * stdDev));
            } else {
                // Normal calculation
                double sum = 0;
                for (int j = 0; j < period; j++) {
                    sum += Math.pow(prices[i - j] - sma.get(i), 2);
                }
                double stdDev = Math.sqrt(sum / period);
                upper.add(sma.get(i) + (stdDevMultiplier * stdDev));
                lower.add(sma.get(i) - (stdDevMultiplier * stdDev));
            }
        }

        Map<String, List<Double>> bands = new HashMap<>();
        bands.put("middle", sma);
        bands.put("upper", upper);
        bands.put("lower", lower);
        
        return bands;
    }
}