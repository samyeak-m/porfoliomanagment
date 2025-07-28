package com.stockmanagment.porfoliomanagment.service.nepse;

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
import java.util.concurrent.CompletableFuture;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.repository.nepse.CustomDailyDataRepository;
import com.stockmanagment.porfoliomanagment.repository.nepse.DailyDataRepository;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    @Autowired
    private EntityManager entityManager;
    
    // Non-blocking WebClient for HTTP requests
    private final WebClient webClient;

    public DailyDataService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB buffer
                .build();
    }

    @PostConstruct
    public void onStartup() {
        System.out.println("Server has started. Preparing to start scraping...");
        startScrapingAfterDelay();
    }

    @Async
    public CompletableFuture<Void> startScrapingAfterDelay() {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(10000);
                scrapeAndStoreDailyDataAsync().subscribe();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Startup scraping interrupted: " + e.getMessage());
            }
        });
    }

    @Scheduled(fixedRate = 60000)
    public void scrapeAndStoreDailyData() {
        scrapeAndStoreDailyDataAsync()
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                result -> System.out.println("Scraping completed successfully"),
                error -> System.err.println("Error during scheduled scraping: " + error.getMessage())
            );
    }

    public Mono<String> scrapeAndStoreDailyDataAsync() {
        return Mono.fromCallable(() -> {
            LocalTime now = LocalTime.now();
            DayOfWeek dayOfWeek = today.getDayOfWeek();

            if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) {
                System.out.println("Market is closed on Friday and Saturday. Sleeping until Sunday.");
                return "Market closed - weekend";
            }

            if (now.isBefore(START_OF_DAY) || now.isAfter(END_OF_DAY)) {
                System.out.println("Market is closed. Skipping scraping.");
                return "Market closed - outside hours";
            }

            return "Market open";
        })
        .flatMap(marketStatus -> {
            if (marketStatus.contains("Market closed")) {
                return Mono.just(marketStatus);
            }
            
            return fetchDataAsync(BASE_URL)
                .flatMap(content -> generateHashAsync(content)
                    .flatMap(currentHash -> {
                        if (!currentHash.equals(lastHash)) {
                            return processAndStoreDataAsync(content)
                                .doOnSuccess(result -> {
                                    lastHash = currentHash;
                                    storeLastUpdateOfTheDay();
                                    System.out.println("Data updated at: " + 
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                                })
                                .then(Mono.just("Data updated successfully"));
                        } else {
                            System.out.println("Data unchanged. Skipping update.");
                            return Mono.just("Data unchanged");
                        }
                    }));
        })
        .onErrorResume(error -> {
            System.err.println("Error during data scraping: " + error.getMessage());
            return Mono.just("Error: " + error.getMessage());
        });
    }

    public Mono<String> fetchDataAsync(String urlStr) {
        return webClient.get()
                .uri(urlStr)
                .header("User-Agent", "Mozilla/5.0")
                .accept(MediaType.TEXT_HTML)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(error -> {
                    System.err.println("Error fetching data from: " + urlStr + " - " + error.getMessage());
                    return Mono.error(new RuntimeException("Failed to fetch data", error));
                });
    }

    public Mono<String> generateHashAsync(String content) {
        return Mono.fromCallable(() -> {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(content.getBytes());
                return Base64.getEncoder().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Error generating hash", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> processAndStoreDataAsync(String content) {
        return Mono.fromCallable(() -> {
            Document doc = Jsoup.parse(content);
            return doc.select("table.table tr");
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(Flux::fromIterable)
        .parallel()
        .runOn(Schedulers.boundedElastic())
        .map(this::parseRowToDaily)
        .filter(dailyData -> dailyData != null)
        .sequential()
        .collectList()
        .flatMap(this::saveAllDailyDataAsync)
        .then();
    }

    private DailyData parseRowToDaily(Element row) {
        try {
            Elements cells = row.select("td");

            if (cells.size() < 21) {
                return null;
            }

            String symbol = cells.get(1).text().trim();
            double open = parseDouble(cells.get(3).text());
            double high = parseDouble(cells.get(4).text());
            double low = parseDouble(cells.get(5).text());
            double close = parseDouble(cells.get(6).text());

            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
            LocalDate localDate = LocalDate.from(timestamp.toLocalDateTime());

            DailyData dailyData = new DailyData();
            dailyData.setDate(localDate);
            dailyData.setSymbol(symbol);
            dailyData.setOpen(Double.valueOf(open));
            dailyData.setHigh(Double.valueOf(high));
            dailyData.setLow(Double.valueOf(low));
            dailyData.setClose(Double.valueOf(close));

            return dailyData;
        } catch (Exception e) {
            System.err.println("Error parsing row: " + e.getMessage());
            return null;
        }
    }

    public Mono<Void> saveAllDailyDataAsync(List<DailyData> dataList) {
        return Mono.fromRunnable(() -> {
            for (DailyData dailyData : dataList) {
                try {
                    DailyData existingData = customDailyDataRepository.getBySymbol(dailyData.getSymbol());
                    
                    if (existingData != null) {
                        existingData.setOpen(dailyData.getOpen());
                        existingData.setHigh(dailyData.getHigh());
                        existingData.setLow(dailyData.getLow());
                        existingData.setClose(dailyData.getClose());
                        existingData.setDate(dailyData.getDate());
                        dailyDataRepository.save(existingData);
                    } else {
                        dailyDataRepository.save(dailyData);
                    }
                } catch (Exception e) {
                    System.err.println("Error saving daily data for symbol " + dailyData.getSymbol() + ": " + e.getMessage());
                }
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    // Reactive methods for other operations
    public Mono<List<DailyData>> getDailyDataBySymbolAndDateRangeAsync(String symbol, LocalDate startDate, LocalDate endDate) {
        return Mono.fromCallable(() -> {
            Timestamp startTimestamp = startDate != null ? Timestamp.valueOf(startDate.atStartOfDay()) : null;
            Timestamp endTimestamp = endDate != null ? Timestamp.valueOf(endDate.atTime(23, 59, 59)) : Timestamp.valueOf(LocalDateTime.now());
            return customDailyDataRepository.getByDateRangeAndSymbol(symbol, startTimestamp, endTimestamp);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<String>> getAllAvailableSymbolsAsync() {
        return Mono.fromCallable(() -> customDailyDataRepository.getAllSymbolsFromDailyData())
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<DailyData>> handleDynamicRequestAsync(String symbol, LocalDate startDate, LocalDate endDate) {
        return Mono.fromCallable(() -> {
            LocalDate effectiveEndDate = endDate;
            if (symbol == null && startDate == null && endDate == null) {
                return getDailyDataBySymbolAndDateRange(null, null, null);
            } else if (symbol != null && startDate == null && endDate == null) {
                return List.of(customDailyDataRepository.getBySymbol(symbol));
            } else if (startDate != null && endDate == null) {
                effectiveEndDate = LocalDate.now(); 
            }
            return getDailyDataBySymbolAndDateRange(symbol, startDate, effectiveEndDate);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<Double>> getStockPriceHistoryAsync(String stockSymbol, int days) {
        return Mono.fromCallable(() -> dailyDataRepository.findPricesForLastNDays(stockSymbol, days))
                .subscribeOn(Schedulers.boundedElastic());
    }

    // Keep synchronous versions for backward compatibility
    public List<DailyData> getDailyDataBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate) {
        Timestamp startTimestamp = startDate != null ? Timestamp.valueOf(startDate.atStartOfDay()) : null;
        Timestamp endTimestamp = endDate != null ? Timestamp.valueOf(endDate.atTime(23, 59, 59)) : Timestamp.valueOf(LocalDateTime.now());
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

    // Utility methods
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

    private int parseInt(String text) {
        try {
            return Integer.parseInt(text.replace(",", "").replace("-", "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void storeLastUpdateOfTheDay() {
        lastUpdateOfTheDay = LocalDateTime.now();
    }
}

