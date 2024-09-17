package com.stockmanagment.porfoliomanagment.controller.home;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.repository.nepse.CustomDailyDataRepository;
import com.stockmanagment.porfoliomanagment.service.nepse.VarCalculationService;
import com.stockmanagment.porfoliomanagment.service.nepse.DailyDataService; // Import DailyDataService
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);


    private final VarCalculationService varCalculationService;
    private final DailyDataService dailyDataService;
    private CustomDailyDataRepository customDailyDataRepository;

    public HomeController(VarCalculationService varCalculationService, DailyDataService dailyDataService) {
        this.varCalculationService = varCalculationService;
        this.dailyDataService = dailyDataService; // Initialize DailyDataService
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/var")
    public String calculateVaR(@RequestParam(required = false) String stockSymbol,
                               @RequestParam(required = false, defaultValue = "25") int days,
                               Model model) {
        if (stockSymbol != null && !stockSymbol.isEmpty()) {
            try {

                double confidenceLevel = varCalculationService.calculateDynamicConfidenceLevel(stockSymbol);
                varCalculationService.calculateAndStoreVaR(stockSymbol, days, confidenceLevel, false);
                double var = varCalculationService.calculateVaR(stockSymbol, days, confidenceLevel);
                double initialStockPrice = varCalculationService.getInitialStockPrice(stockSymbol);

                double varPercentage = (var / initialStockPrice) * 100;

                model.addAttribute("var", String.format("%.2f", var));
                model.addAttribute("confidenceLevel", String.format("%.2f", confidenceLevel * 100));
                model.addAttribute("initialStockPrice", String.format("%.2f", initialStockPrice));
                model.addAttribute("varPercentage", String.format("%.2f", varPercentage));
                model.addAttribute("stockSymbol", stockSymbol);
            } catch (RuntimeException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "var/calculate-var";
    }

    @GetMapping("/daily")
    public String getDailyData(@RequestParam(required = false) String symbol,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate,
                               Model model) {

        try {

            if (symbol != null && !symbol.isEmpty()) {
                if (startDate != null && endDate != null) {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    // Convert LocalDate to Timestamp
                    Timestamp startTimestamp = Timestamp.valueOf(start.atStartOfDay());
                    Timestamp endTimestamp = Timestamp.valueOf(end.plusDays(1).atStartOfDay());

                    List<DailyData> dailyDataList = customDailyDataRepository.getByDateRangeAndSymbol(symbol, startTimestamp, endTimestamp);
                    model.addAttribute("dailyData", dailyDataList);
                } else {
                    DailyData dailyData = customDailyDataRepository.getBySymbol(symbol);
                    model.addAttribute("dailyData", dailyData);
                }
            } else {
                model.addAttribute("error", "Symbol is required");
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "daily/dailydata";
    }

}
