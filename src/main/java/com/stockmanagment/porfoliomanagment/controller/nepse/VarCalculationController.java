package com.stockmanagment.porfoliomanagment.controller.nepse;

import com.stockmanagment.porfoliomanagment.service.nepse.VarCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/var")
public class VarCalculationController {

    private final VarCalculationService varCalculationService;

    @Autowired
    public VarCalculationController(VarCalculationService varCalculationService) {
        this.varCalculationService = varCalculationService;
    }

    // Single stock VaR calculation
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
            response.put("confidenceLevel", confidenceLevel * 100);  // Confidence Level as a percentage
            response.put("initialPrice", initialPrice);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);  // Return HTTP 500 on error
        }
    }

    @PostMapping("/mulcalculate")
    public ResponseEntity<Map<String, Object>> calculateMultipleVaRBasedOnCriteria(@RequestParam int daysOfInvestment) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Call the service method to get VaR for eligible stocks
            Map<String, Object> varResults = varCalculationService.calculateMultipleVaRBasedOnInvestmentCriteria(daysOfInvestment, 0.95); // Default confidence level (adjustable)

            response.put("stocks", varResults);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error processing request: " + e.getMessage());
            return ResponseEntity.status(500).body(response);  // Ensuring JSON response even in error cases
        }
    }


}
