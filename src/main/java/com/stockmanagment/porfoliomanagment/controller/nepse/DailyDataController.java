package com.stockmanagment.porfoliomanagment.controller.nepse;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.repository.nepse.CustomDailyDataRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@RestController
@RequestMapping("/api/daily-data")
public class DailyDataController {

    @Autowired
    private CustomDailyDataRepository customDailyDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

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

    @GetMapping("/data")
    public ResponseEntity<List<DailyData>> getByDateRangeAndSymbol(
            @RequestParam(value = "symbol", required = false) String symbol,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            if (startDate == null) {
                startDate = LocalDate.now();
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            Timestamp startTimestamp = Timestamp.valueOf(startDate.atStartOfDay());
            Timestamp endTimestamp = Timestamp.valueOf(endDate.atTime(23, 59, 59));

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

    /**
     * Always read from shared daily_data table (JPA entity) for the given symbol.
     * Use this endpoint when the client provides only symbol (no date range).
     */
    @GetMapping("/shared-by-symbol")
    public ResponseEntity<List<DailyData>> getSharedDailyDataBySymbol(@RequestParam("symbol") String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String sym = symbol.replace('/', '_').toLowerCase();
        List<DailyData> list = entityManager.createQuery(
                "SELECT d FROM DailyData d WHERE LOWER(d.symbol)=:sym ORDER BY d.date ASC",
                DailyData.class)
                .setParameter("sym", sym)
                .getResultList();
        return ResponseEntity.ok(list);
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
