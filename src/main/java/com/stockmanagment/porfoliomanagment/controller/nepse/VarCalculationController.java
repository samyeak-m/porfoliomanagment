package com.stockmanagment.porfoliomanagment.controller.nepse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockmanagment.porfoliomanagment.service.nepse.VarCalculationService;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.database.DatabaseHelper;

@RestController
@RequestMapping("/api/var")
public class VarCalculationController {

    private final VarCalculationService varCalculationService;

    @Autowired
    public VarCalculationController(VarCalculationService varCalculationService) {
        this.varCalculationService = varCalculationService;
    }ResponseEntity<List<String>> getStockSymbols() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Use DatabaseHelper directly instead of cache service
            DatabaseHelper dbHelper = new DatabaseHelper();
            List<String> symbols = dbHelper.getAllStockTableNames();
            
            // Convert to uppercase and sort
            List<String> sortedSymbols = symbols.stream()
                    .map(String::toUpperCase)
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("Fetched " + sortedSymbols.size() + " symbols in " + duration + "ms");
            
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                    .body(sortedSymbols);
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("Error fetching stock symbols in " + duration + "ms: " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/calculate/{stockSymbol}")
    public ResponseEntity<Map<String, Object>> calculateVaR(@PathVariable String stockSymbol,
                                                            @RequestParam(required = false, defaultValue = "25") int daysOfInvestment) {
        Map<String, Object> response = new HashMap<>();
        try {
            double confidenceLevel = varCalculationService.calculateDynamicConfidenceLevel(stockSymbol);
            double varValue = varCalculationService.calculateVaR(stockSymbol, daysOfInvestment, confidenceLevel);
            double initialPrice = varCalculationService.getInitialStockPrice(stockSymbol);

            response.put("stockSymbol", stockSymbol);
            response.put("varValue", varValue);
            response.put("confidenceLevel", confidenceLevel * 100);
            response.put("initialPrice", initialPrice);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response); 
        }
    }

    @PostMapping("/populate")
    public ResponseEntity<String> populateVarData() {
        try {
            // Force repopulation regardless of existing data
            long startTime = System.currentTimeMillis();
            
            // Get all symbols and calculate VaR
            java.util.List<String> symbols = varCalculationService.getAllAvailableStockSymbols();
            int processed = 0;
            
            for (String symbol : symbols) {
                try {
                    java.util.List<Double> closePrices = varCalculationService.getClosePrices(symbol);
                    if (closePrices.size() >= 30) {
                        java.util.Random random = new java.util.Random();
                        int days = 25 + random.nextInt(200);
                        double confidence = 0.90 + (0.09 * random.nextDouble());
                        varCalculationService.calculateAndStoreVaR(symbol, days, confidence, true);
                        processed++;
                    }
                } catch (Exception e) {
                    System.err.println("Error processing " + symbol + ": " + e.getMessage());
                }
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            String message = String.format("Successfully populated VaR data for %d symbols in %d ms", 
                    processed, elapsed);
            
            return ResponseEntity.ok(message);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error populating VaR data: " + e.getMessage());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<String> getVarDataCount() {
        try {
            long count = varCalculationService.getVarOfAllDataRepository().count();
            return ResponseEntity.ok("VarOfAllData table contains " + count + " records");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error getting count: " + e.getMessage());
        }
    }

    @GetMapping("/var")
    public String calculateVaR(@RequestParam(required = false) String stockSymbol,
                               @RequestParam(required = false, defaultValue = "25") int days,
                               Model model) {
        if (stockSymbol != null && !stockSymbol.isEmpty()) {
            String normalizedStockSymbol = stockSymbol.replace('/', '_').toLowerCase();
            try {
                // FIXED: Pre-validate symbol has some data
                List<Double> prices = varCalculationService.getClosePrices(normalizedStockSymbol);
                if (prices.isEmpty()) {
                    model.addAttribute("warning", "No historical data found for " + stockSymbol + ". Using estimated values.");
                }
                
                double confidenceLevel = varCalculationService.calculateDynamicConfidenceLevel(normalizedStockSymbol);
                varCalculationService.calculateAndStoreVaR(normalizedStockSymbol, days, confidenceLevel, false);
                double var = varCalculationService.calculateVaR(normalizedStockSymbol, days, confidenceLevel);
                double initialStockPrice = varCalculationService.getInitialStockPrice(normalizedStockSymbol);

                double varPercentage = (var / initialStockPrice) * 100;

                model.addAttribute("var", String.format("%.2f", var));
                model.addAttribute("confidenceLevel", String.format("%.2f", confidenceLevel * 100));
                model.addAttribute("initialStockPrice", String.format("%.2f", initialStockPrice));
                model.addAttribute("varPercentage", String.format("%.2f", varPercentage));
                model.addAttribute("stockSymbol", normalizedStockSymbol);
            } catch (RuntimeException e) {
                model.addAttribute("error", "Error calculating VaR: " + e.getMessage());
                System.err.println("VaR calculation error for " + stockSymbol + ": " + e.getMessage());
            }
        }
        return "var/calculate-var";
    }
}
