package com.stockmanagment.porfoliomanagment.controller.trading;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.stockmanagment.porfoliomanagment.service.nepse.TradingChartService;

@Controller
@RequestMapping("/trading")
public class TradingController {

    private final TradingChartService tradingChartService;

    public TradingController(TradingChartService tradingChartService) {
        this.tradingChartService = tradingChartService;
    }

    @GetMapping("/chart")
    public String showChartPage() {
        return "trading/chart";
    }

    @GetMapping("/api/symbols")
    public ResponseEntity<List<String>> getAvailableSymbols() {
        try {
            List<String> symbols = tradingChartService.getAvailableSymbols();
            return ResponseEntity.ok(symbols);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/chart-data")
    public ResponseEntity<Map<String, Object>> getChartData(
            @RequestParam String symbol) {
        try {
            // FIXED: Load ALL data for the symbol (no timeframe filtering)
            Map<String, Object> chartData = tradingChartService.getChartDataForSymbol(symbol);
            return ResponseEntity.ok(chartData);
        } catch (Exception e) {
            System.err.println("Error fetching chart data: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}