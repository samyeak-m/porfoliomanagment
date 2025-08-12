package com.stockmanagment.porfoliomanagment.controller.nepse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
