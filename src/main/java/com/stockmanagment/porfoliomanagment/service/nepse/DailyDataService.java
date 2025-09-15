package com.stockmanagment.porfoliomanagment.service.nepse;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.repository.nepse.CustomDailyDataRepository;
import com.stockmanagment.porfoliomanagment.repository.nepse.DailyDataRepository;

import jakarta.annotation.PostConstruct;

@Service
public class DailyDataService {

    @Value("${scraper.base-url}")
    private String BASE_URL;

    private static final LocalTime START_OF_DAY = LocalTime.of(10, 45);
    private static final LocalTime END_OF_DAY = LocalTime.of(15, 15);
    private String lastHash = "";
    private LocalDateTime lastUpdateOfTheDay;

    @Autowired
    private DailyDataRepository dailyDataRepository;

    @Autowired
    private CustomDailyDataRepository customDailyDataRepository;

    // NEW: log gating to avoid spam during closed hours
    private LocalDate lastClosedLogDay = null;
    private boolean openNotified = false;

    @PostConstruct
    public void onStartup() {
        System.out.println("Server has started. Preparing to start scraping...");
    }

    // Run every 5 minutes, non-blocking
    @Scheduled(cron = "0 */5 * * * *")
    public void scrapeAndStoreDailyData() {
        try {
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            if (!isMarketOpen(today, now)) {
                if (lastClosedLogDay == null || !lastClosedLogDay.isEqual(today)) {
                    LocalDateTime nextOpen = getNextMarketOpen(today, now);
                    System.out.println("Market is closed. Next open at: " +
                            nextOpen.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    lastClosedLogDay = today;
                    openNotified = false;
                    replicateToPerSymbolTables();
                }
                return;
            }

            if (!openNotified) {
                System.out.println("Market is open. Starting scraping cycle.");
                openNotified = true;
                lastClosedLogDay = null;
            }

            String content = fetchData(BASE_URL);
            String currentHash = generateHash(content);

            if (!currentHash.equals(lastHash)) {
                processAndStoreData(content);
                lastHash = currentHash;
                storeLastUpdateOfTheDay();
                System.out.println("Data updated at: "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        } catch (Exception e) {
            System.err.println("Error during data scraping: " + e.getMessage());
        }
    }

    private String fetchData(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        return new String(conn.getInputStream().readAllBytes());
    }

    private String generateHash(String content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    // NEW: sanitize only slash to underscore for daily_data storage
    private String sanitizeSymbol(String symbol) {
        return symbol == null ? null : symbol.replace('/', '_');
    }

    private void processAndStoreData(String content) {
        Document doc = Jsoup.parse(content);
        for (Element row : doc.select("table.table tr")) {
            Elements cells = row.select("td");

            if (cells.size() < 21) {
                continue;
            }

            String rawSymbol = cells.get(1).text().trim();
            String symbol = sanitizeSymbol(rawSymbol);

            double open = parseDouble(cells.get(3).text());
            double high = parseDouble(cells.get(4).text());
            double low = parseDouble(cells.get(5).text());
            double close = parseDouble(cells.get(6).text());

            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
            LocalDate localDate = LocalDate.from(timestamp.toLocalDateTime());

            // EXISTING: Update daily_data
            DailyData existingData = customDailyDataRepository.getBySymbol(symbol);
            if (existingData != null) {
                existingData.setOpen(Double.valueOf(open));
                existingData.setHigh(Double.valueOf(high));
                existingData.setLow(Double.valueOf(low));
                existingData.setClose(Double.valueOf(close));
                existingData.setDate(localDate);
                dailyDataRepository.save(existingData);
            } else {
                DailyData dailyData = new DailyData();
                dailyData.setDate(localDate);
                dailyData.setSymbol(symbol);
                dailyData.setOpen(Double.valueOf(open));
                dailyData.setHigh(Double.valueOf(high));
                dailyData.setLow(Double.valueOf(low));
                dailyData.setClose(Double.valueOf(close));
                dailyDataRepository.save(dailyData);
            }

            // NEW: Also store in live_data for real-time tracking
            storeLiveData(symbol, open, high, low, close, timestamp);
        }
    }

    // NEW: Store live data with current timestamp
    private void storeLiveData(String symbol, double open, double high, double low, double close, Timestamp timestamp) {
        try (var conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/porfoliomanagment_nepse", 
                "root", "")) {
            
            String sql = """
                INSERT INTO live_data (symbol, open, high, low, close, timestamp, date)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    high = GREATEST(high, VALUES(high)),
                    low = LEAST(low, VALUES(low)),
                    close = VALUES(close),
                    timestamp = VALUES(timestamp)
                """;
                
            try (var pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, symbol);
                pstmt.setDouble(2, open);
                pstmt.setDouble(3, high);
                pstmt.setDouble(4, low);
                pstmt.setDouble(5, close);
                pstmt.setTimestamp(6, timestamp);
                pstmt.setDate(7, new java.sql.Date(timestamp.getTime()));
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("Error storing live data for " + symbol + ": " + e.getMessage());
        }
    }

    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text.replace(",", "").replace("-", "0"));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public void storeLastUpdateOfTheDay() {
        lastUpdateOfTheDay = LocalDateTime.now();
    }

    public List<DailyData> getDailyDataBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate) {
        Timestamp startTimestamp = startDate != null ? Timestamp.valueOf(startDate.atStartOfDay()) : null;
        Timestamp endTimestamp = endDate != null ? Timestamp.valueOf(endDate.atTime(23, 59, 59))
                : Timestamp.valueOf(LocalDateTime.now());
        return customDailyDataRepository.getByDateRangeAndSymbol(symbol, startTimestamp, endTimestamp);
    }

    public List<String> getAllAvailableSymbols() {
        return customDailyDataRepository.getAllSymbolsFromDailyData();
    }

    public List<DailyData> handleDynamicRequest(String symbol, LocalDate startDate, LocalDate endDate) {
        if (symbol == null && startDate == null && endDate == null) {
            return getDailyDataBySymbolAndDateRange(null, null, null);
        } else if (symbol != null && startDate == null && endDate == null) {
            return List.of(customDailyDataRepository.getBySymbol(symbol));
        } else if (startDate != null && endDate == null) {
            endDate = LocalDate.now(); 
        }
        return getDailyDataBySymbolAndDateRange(symbol, startDate, endDate);
    }

    public List<Double> getStockPriceHistory(String stockSymbol, int days) {
        return dailyDataRepository.findPricesForLastNDays(stockSymbol, days);
    }

    // NEW: Safe fetch of latest OHLC from shared daily_data table
    @Transactional(readOnly = true)
    public DailyData getLatestSharedDailyData(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) return null;
        String symbol = rawSymbol.replace('/', '_').toLowerCase();
        // Use custom repo if it already has a method, otherwise add one native query
        try {
            return customDailyDataRepository.getLatestBySymbol(symbol);
        } catch (Exception e) {
            return null;
        }
    }

    // OPTIONAL: remove any legacy per-symbol access before calling process/store
    // If you had a method that looked up daily_data_<symbol>, refactor it to call getLatestSharedDailyData()

    // NEW: market hours helper
    private boolean isMarketOpen(LocalDate date, LocalTime time) {
        DayOfWeek dow = date.getDayOfWeek();
        boolean weekend = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY);
        if (weekend) return false;
        return !time.isBefore(START_OF_DAY) && !time.isAfter(END_OF_DAY);
    }

    // NEW: compute next market open datetime (Sun–Thu 10:45)
    private LocalDateTime getNextMarketOpen(LocalDate date, LocalTime time) {
        LocalDate d = date;
        if (time.isAfter(END_OF_DAY)) {
            d = d.plusDays(1);
        }
        if (time.isBefore(START_OF_DAY)) {
            // same day is fine if not weekend
        }
        // advance to next working day (Sun–Thu)
        while (d.getDayOfWeek() == DayOfWeek.FRIDAY || d.getDayOfWeek() == DayOfWeek.SATURDAY) {
            d = d.plusDays(1);
        }
        return LocalDateTime.of(d, START_OF_DAY);
    }

    // NEW: Replicate shared daily_data to per-symbol tables when market closes
    private void replicateToPerSymbolTables() {
        try {
            List<String> symbols = customDailyDataRepository.getAllSymbolsFromDailyData();
            System.out.println("Replicating data to " + symbols.size() + " per-symbol tables...");
            
            for (String symbol : symbols) {
                createAndPopulateSymbolTable(symbol);
            }
            
            System.out.println("Data replication completed successfully.");
        } catch (Exception e) {
            System.err.println("Error during data replication: " + e.getMessage());
        }
    }

    // Create and populate per-symbol table with data from shared table
    private void createAndPopulateSymbolTable(String symbol) {
        String sanitizedSymbol = sanitizeSymbol(symbol);
        String tableName = "daily_data_" + sanitizedSymbol;
        
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS %s (
                id INT AUTO_INCREMENT PRIMARY KEY,
                date DATE NOT NULL,
                open DECIMAL(10,2),
                high DECIMAL(10,2),
                low DECIMAL(10,2),
                close DECIMAL(10,2),
                UNIQUE KEY unique_date (date)
            )
            """.formatted(tableName);
        
        String insertDataSQL = """
            INSERT INTO %s (date, open, high, low, close)
            SELECT date, open, high, low, close 
            FROM daily_data 
            WHERE LOWER(symbol) = ?
            ON DUPLICATE KEY UPDATE
                open = VALUES(open),
                high = VALUES(high),
                low = VALUES(low),
                close = VALUES(close)
            """.formatted(tableName);
        
        try (var conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/porfoliomanagment_nepse", 
                "root", "")) {
            
            // Create table
            try (var stmt = conn.createStatement()) {
                stmt.executeUpdate(createTableSQL);
            }
            
            // Insert/update data
            try (var pstmt = conn.prepareStatement(insertDataSQL)) {
                pstmt.setString(1, sanitizedSymbol.toLowerCase());
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Updated " + rowsAffected + " rows for " + tableName);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error creating/updating table " + tableName + ": " + e.getMessage());
        }
    }
}
