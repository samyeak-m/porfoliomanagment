package com.stockmanagment.porfoliomanagment.service.nepse;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
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
    private static final LocalDate today = LocalDate.now();
    private String lastHash = "";
    private LocalDateTime lastUpdateOfTheDay;

    @Autowired
    private DailyDataRepository dailyDataRepository;

    @Autowired
    private CustomDailyDataRepository customDailyDataRepository;

    @PostConstruct
    public void onStartup() {
        System.out.println("Server has started. Preparing to start scraping...");
        startScrapingAfterDelay();
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
                System.out.println("Data updated at: "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else {
                System.out.println("Data unchanged. Skipping update.");
            }

            Thread.sleep(getSleepDuration());
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
            String symbol = sanitizeSymbol(rawSymbol); // CHANGED: use sanitized symbol

            double open = parseDouble(cells.get(3).text());
            double high = parseDouble(cells.get(4).text());
            double low = parseDouble(cells.get(5).text());
            double close = parseDouble(cells.get(6).text());

            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
            LocalDate localDate = LocalDate.from(timestamp.toLocalDateTime());

            // CHANGED: find by sanitized symbol
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
                dailyData.setSymbol(symbol); // CHANGED: save sanitized symbol
                dailyData.setOpen(Double.valueOf(open));
                dailyData.setHigh(Double.valueOf(high));
                dailyData.setLow(Double.valueOf(low));
                dailyData.setClose(Double.valueOf(close));
                dailyDataRepository.save(dailyData);
            }
        }
    }

    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text.replace(",", "").replace("-", "0"));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private long getSleepDuration() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(START_OF_DAY)) {
            return Duration.between(now, START_OF_DAY).toMillis();
        } else if (now.isAfter(END_OF_DAY)) {
            return Duration.between(now, START_OF_DAY.plusHours(24)).toMillis();
        }
        return 60000;
    }

    private long getSleepDurationUntilSunday() {
        LocalDate today = LocalDate.now();
        LocalDate nextSunday = today.with(DayOfWeek.SUNDAY);
        return Duration.between(LocalDateTime.now(), LocalDateTime.of(nextSunday, START_OF_DAY)).toMillis();
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

}
