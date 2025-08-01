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

import com.stockmanagment.porfoliomanagment.service.StockSymbolCacheService;
import com.stockmanagment.porfoliomanagment.service.nepse.VarCalculationService;

@RestController
@RequestMapping("/api/var")
public class VarCalculationController {

    @Autowired
    private StockSymbolCacheService stockSymbolCacheService;

    private final VarCalculationService varCalculationService;

    @Autowired
    public VarCalculationController(VarCalculationService varCalculationService) {
        this.varCalculationService = varCalculationService;
    }

    @GetMapping("/stock-symbols")
    public ResponseEntity<List<String>> getStockSymbols() {
        long startTime = System.currentTimeMillis();
        
        try {
            List<String> symbols = stockSymbolCacheService.getCachedStockSymbols();
            
            long duration = System.currentTimeMillis() - startTime;
            
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                    .body(symbols);
                    
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
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
