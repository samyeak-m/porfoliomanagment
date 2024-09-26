package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.repository.nepse.CustomDailyDataRepository;
import com.stockmanagment.porfoliomanagment.repository.nepse.DailyDataRepository;
import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DailyDataService {

    @Value("${scraper.base-url}")
    private String BASE_URL;

    private static final LocalTime START_OF_DAY = LocalTime.of(10, 45);
    private static final LocalTime END_OF_DAY = LocalTime.of(15, 15);
    private static final LocalDate today = LocalDate.now();
    private String lastHash = ""; // To store hash of last fetched data
    private LocalDateTime lastUpdateOfTheDay; // To track the last update

    @Autowired
    private DailyDataRepository dailyDataRepository;

    @Autowired
    private CustomDailyDataRepository customDailyDataRepository;

    @Autowired
    private EntityManager entityManager; // EntityManager for native query execution

    @PostConstruct
    public void onStartup() {
//        System.out.println("Server has started. Preparing to start scraping...");
        // Delayed scraping to allow server to fully start before data scraping begins.
//        startScrapingAfterDelay();
    }

    public void startScrapingAfterDelay() {
        new Thread(() -> {
            try {
                Thread.sleep(10000);
                scrapeAndStoreDailyData();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Scheduled(fixedRate = 60000)
    public void scrapeAndStoreDailyData() {
        try {
            LocalTime now = LocalTime.now();
            DayOfWeek dayOfWeek = today.getDayOfWeek();

            if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) {
                System.out.println("Market is closed on Friday and Saturday. Sleeping until Sunday.");
                Thread.sleep(getSleepDurationUntilSunday());
                return;
            }

            if (now.isBefore(START_OF_DAY) || now.isAfter(END_OF_DAY)) {
                System.out.println("Market is closed. Skipping scraping.");
                return;
            }

            String content = fetchData(BASE_URL);
            String currentHash = generateHash(content);

            if (!currentHash.equals(lastHash)) {
                processAndStoreData(content);
                lastHash = currentHash;
                storeLastUpdateOfTheDay();
                System.out.println("Data updated at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else {
                System.out.println("Data unchanged. Skipping update.");
            }

            Thread.sleep(getSleepDuration());
        } catch (Exception e) {
            System.err.println("Error during data scraping: " + e.getMessage());
        }
    }

    // Fetch data from the given URL
    private String fetchData(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        return new String(conn.getInputStream().readAllBytes());
    }

    // Generate a hash for the fetched content to detect changes
    private String generateHash(String content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    // Process the scraped HTML content and store the data
    private void processAndStoreData(String content) {
        Document doc = Jsoup.parse(content);
        for (Element row : doc.select("table.table tr")) {
            Elements cells = row.select("td");

            if (cells.size() < 21) {
                continue; // Skip rows that don't have enough data
            }

            String symbol = cells.get(1).text().trim();
            double open = parseDouble(cells.get(3).text());
            double high = parseDouble(cells.get(4).text());
            double low = parseDouble(cells.get(5).text());
            double close = parseDouble(cells.get(6).text());

            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now()); // Current timestamp with date and time

            LocalDate localDate = LocalDate.from(timestamp.toLocalDateTime());

            DailyData existingData = customDailyDataRepository.getBySymbol(symbol);

            if (existingData != null) {
                // Update existing record
                existingData.setOpen(Double.valueOf(open));
                existingData.setHigh(Double.valueOf(high));
                existingData.setLow(Double.valueOf(low));
                existingData.setClose(Double.valueOf(close));
                existingData.setDate(localDate);
                // Set other fields as needed
                dailyDataRepository.save(existingData);
            } else {
                // Insert new record
                DailyData dailyData = new DailyData();
                dailyData.setDate(localDate);
                dailyData.setSymbol(symbol);
                dailyData.setOpen(Double.valueOf(open));
                dailyData.setHigh(Double.valueOf(high));
                dailyData.setLow(Double.valueOf(low));
                dailyData.setClose(Double.valueOf(close));
                // Set other fields as needed
                dailyDataRepository.save(dailyData);
            }
        }
    }

    // Parse a double from a string with handling for missing or malformed values
    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text.replace(",", "").replace("-", "0"));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Get sleep duration between scraping operations, handling market open/close
    private long getSleepDuration() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(START_OF_DAY)) {
            return Duration.between(now, START_OF_DAY).toMillis();
        } else if (now.isAfter(END_OF_DAY)) {
            return Duration.between(now, START_OF_DAY.plusHours(24)).toMillis();
        }
        return 60000; // Sleep for 60 seconds if market is open
    }

    // Get sleep duration until the next Sunday for weekly market closure handling
    private long getSleepDurationUntilSunday() {
        LocalDate today = LocalDate.now();
        LocalDate nextSunday = today.with(DayOfWeek.SUNDAY);
        return Duration.between(LocalDateTime.now(), LocalDateTime.of(nextSunday, START_OF_DAY)).toMillis();
    }

    // Parse integer with handling for non-numeric inputs (if needed)
    private int parseInt(String text) {
        try {
            return Integer.parseInt(text.replace(",", "").replace("-", "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Store the last update time of the day (to track scraping on a daily basis)
    public void storeLastUpdateOfTheDay() {
        lastUpdateOfTheDay = LocalDateTime.now();
    }

    public List<DailyData> getDailyDataBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate) {
        Timestamp startTimestamp = startDate != null ? Timestamp.valueOf(startDate.atStartOfDay()) : null;
        Timestamp endTimestamp = endDate != null ? Timestamp.valueOf(endDate.atTime(23, 59, 59)) : Timestamp.valueOf(LocalDateTime.now());
        return customDailyDataRepository.getByDateRangeAndSymbol(symbol, startTimestamp, endTimestamp);
    }

    public List<String> getAllAvailableSymbols() {
        return customDailyDataRepository.getAllSymbolsFromDailyData();
    }

    // Handle dynamic requests
    public List<DailyData> handleDynamicRequest(String symbol, LocalDate startDate, LocalDate endDate) {
        if (symbol == null && startDate == null && endDate == null) {
            return getDailyDataBySymbolAndDateRange(null, null, null); // Load all data
        } else if (symbol != null && startDate == null && endDate == null) {
            return List.of(customDailyDataRepository.getBySymbol(symbol));
        } else if (startDate != null && endDate == null) {
            endDate = LocalDate.now(); // Set end date to current date
        }
        return getDailyDataBySymbolAndDateRange(symbol, startDate, endDate);
    }

}

