package com.stockmanagment.porfoliomanagment.controller.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.service.nepse.DailyDataService;
import com.stockmanagment.porfoliomanagment.repository.nepse.CustomDailyDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public ResponseEntity<DailyData> getBySymbol(@PathVariable String symbol) {
        try {
            DailyData dailyData = customDailyDataRepository.getBySymbol(symbol);
            if (dailyData == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(dailyData, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error fetching data by symbol: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get daily data by date range and symbol (dynamic table)
    @GetMapping("/data")
    public ResponseEntity<List<DailyData>> getByDateRangeAndSymbol(
            @RequestParam(value = "symbol", required = false) String symbol,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // If no dates are provided, use the current date for both start and end
            if (startDate == null) {
                startDate = LocalDate.now();
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            // Convert LocalDate to Timestamp
            Timestamp startTimestamp = Timestamp.valueOf(startDate.atStartOfDay());
            Timestamp endTimestamp = Timestamp.valueOf(endDate.atTime(23, 59, 59));

            // If symbol is not provided, query all symbols
            List<DailyData> dailyDataList = customDailyDataRepository.getByDateRangeAndSymbol(symbol, startTimestamp, endTimestamp);
            if (dailyDataList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(dailyDataList, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error fetching data by date range and symbol: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/today")
    public ResponseEntity<List<DailyData>> getTodaysData() {
        try {
            // Fetch today's data directly
            List<DailyData> todaysData = customDailyDataRepository.getByDate();
            if (todaysData.isEmpty()) {
                System.err.println("No data found for today.");
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(todaysData, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error fetching today's data: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


//    // Trigger data scraping manually
//    @PostMapping("/scrape")
//    public ResponseEntity<String> scrapeDailyData() {
//        try {
//            dailyDataService.scrapeAndStoreDailyData();
//            return ResponseEntity.ok("Data scraping and storage triggered successfully.");
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during data scraping: " + e.getMessage());
//        }
//    }

    // Store the last update of the day
//    @PostMapping("/store-last-update")
//    public ResponseEntity<String> storeLastUpdateOfTheDay() {
//        try {
//            dailyDataService.storeLastUpdateOfTheDay();
//            return ResponseEntity.ok("Last update of the day stored successfully.");
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error storing last update: " + e.getMessage());
//        }
//    }
}
