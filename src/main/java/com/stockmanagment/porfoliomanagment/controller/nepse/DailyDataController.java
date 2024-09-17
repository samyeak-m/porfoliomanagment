package com.stockmanagment.porfoliomanagment.controller.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.service.nepse.DailyDataService;
import com.stockmanagment.porfoliomanagment.repository.nepse.CustomDailyDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.sql.Timestamp;

@RestController
@RequestMapping("/api/daily-data")
public class DailyDataController {

    @Autowired
    private DailyDataService dailyDataService;

    @Autowired
    private CustomDailyDataRepository customDailyDataRepository;

    // Get daily data by symbol
    @GetMapping("/symbol/{symbol}")
    public DailyData getBySymbol(String symbol) {
        try {
            return customDailyDataRepository.getBySymbol(symbol);
        } catch (Exception e) {
            System.err.println("Error fetching data by symbol: " + e.getMessage());
            return null;
        }
    }

    // Get daily data by date range and symbol (dynamic table)
    @GetMapping("/symbol/{symbol}/date-range")
    public List<DailyData> getByDateRangeAndSymbol(@PathVariable String symbol,
                                                   @RequestParam("startDate") LocalDate startDate,
                                                   @RequestParam("endDate") LocalDate endDate) {
        try {
            // Convert LocalDate to Timestamp
            Timestamp startTimestamp = Timestamp.valueOf(startDate.atStartOfDay());
            Timestamp endTimestamp = Timestamp.valueOf(endDate.plusDays(1).atStartOfDay());

            return customDailyDataRepository.getByDateRangeAndSymbol(symbol, startTimestamp, endTimestamp);
        } catch (Exception e) {
            System.err.println("Error fetching data by date range and symbol: " + e.getMessage());
            return List.of();
        }
    }


    // Trigger data scraping manually
    @PostMapping("/scrape")
    public ResponseEntity<String> scrapeDailyData() {
        try {
            dailyDataService.scrapeAndStoreDailyData();
            return ResponseEntity.ok("Data scraping and storage triggered successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during data scraping: " + e.getMessage());
        }
    }

    // Store the last update of the day
    @PostMapping("/store-last-update")
    public ResponseEntity<String> storeLastUpdateOfTheDay() {
        try {
            dailyDataService.storeLastUpdateOfTheDay();
            return ResponseEntity.ok("Last update of the day stored successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error storing last update: " + e.getMessage());
        }
    }
}
