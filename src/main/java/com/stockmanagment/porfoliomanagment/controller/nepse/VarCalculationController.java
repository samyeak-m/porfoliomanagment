package com.stockmanagment.porfoliomanagment.controller.nepse;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockmanagment.porfoliomanagment.service.nepse.VarCalculationService;

@RestController
@RequestMapping("/api/var")
public class VarCalculationController {

    private final VarCalculationService varCalculationService;

    @Autowired
    public VarCalculationController(VarCalculationService varCalculationService) {
        this.varCalculationService = varCalculationService;
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

    @PostMapping("/mulcalculate")
    public ResponseEntity<Map<String, Object>> calculateMultipleVaRBasedOnCriteria(@RequestParam int daysOfInvestment) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> varResults = varCalculationService.calculateMultipleVaRBasedOnInvestmentCriteria(daysOfInvestment, 0.95);

            response.put("stocks", varResults);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error processing request: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }


}
