package com.stockmanagment.porfoliomanagment.controller.nepse;

import com.stockmanagment.porfoliomanagment.service.nepse.VarCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/var")
public class VarCalculationController {

    @Autowired

    private final VarCalculationService varCalculationService;

    public VarCalculationController(VarCalculationService varCalculationService) {
        this.varCalculationService = varCalculationService;
    }

    @GetMapping("/calculate/{stockSymbol}")
    public String calculateVaR(@PathVariable String stockSymbol,
                               @RequestParam(required = false, defaultValue = "25") int daysOfInvestment) {
        try {
            double confidenceLevel = varCalculationService.calculateDynamicConfidenceLevel(stockSymbol);
            varCalculationService.calculateAndStoreVaR(stockSymbol, daysOfInvestment, confidenceLevel, false);
            return "VaR calculation successful for stock: " + stockSymbol + " with Confidence Level: " + confidenceLevel;
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }
}
