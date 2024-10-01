package com.stockmanagment.porfoliomanagment.controller.home;
import com.stockmanagment.porfoliomanagment.service.nepse.PredictionsService;

import com.stockmanagment.porfoliomanagment.model.portfolio.UserDetail;
import com.stockmanagment.porfoliomanagment.service.portfolio.UserDetailService;
import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.repository.nepse.CustomDailyDataRepository;
import com.stockmanagment.porfoliomanagment.service.nepse.DailyDataService;
import com.stockmanagment.porfoliomanagment.service.nepse.VarCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final VarCalculationService varCalculationService;
    private final DailyDataService dailyDataService;
    private final CustomDailyDataRepository customDailyDataRepository;
    private final UserDetailService userDetailService;
    private final PredictionsService predictionsService;

    private final Map<String, Double> nextClosePriceCache = new HashMap<>();
    private Map<String, Double> previousPredictions = new HashMap<>();



    public HomeController(VarCalculationService varCalculationService, DailyDataService dailyDataService,
                          CustomDailyDataRepository customDailyDataRepository, UserDetailService userDetailService, PredictionsService predictionsService) {
        this.varCalculationService = varCalculationService;
        this.dailyDataService = dailyDataService;
        this.customDailyDataRepository = customDailyDataRepository;
        this.userDetailService = userDetailService;
        this.predictionsService = predictionsService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, Model model) {
        Optional<UserDetail> userDetail = userDetailService.login(email, password);
        if (userDetail.isPresent()) {
//            session.setAttribute("user", userDetail.get());
            return "daily/dailydata";
        }
        model.addAttribute("error", "Invalid email or password");
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String email,
                         @RequestParam String password,
                         @RequestParam String phone,
                         Model model) {
        UserDetail newUser = new UserDetail();
        newUser.setEmail(email);
        newUser.setPassword(password);
        newUser.setPhone(phone);

        userDetailService.saveUserDetail(newUser);

        model.addAttribute("message", "Signup successful! Please login.");
        return "auth/login";
    }



    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login";
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

                LocalDate today = LocalDate.now();
                String cacheKey = stockSymbol + today;

                double nextClosePrice = nextClosePriceCache.computeIfAbsent(cacheKey, key -> calculateNextClosePrice(stockSymbol, initialStockPrice));

                double varPercentage = (var / initialStockPrice) * 100;

                model.addAttribute("var", String.format("%.2f", var));
                model.addAttribute("confidenceLevel", String.format("%.2f", confidenceLevel * 100));
                model.addAttribute("initialStockPrice", String.format("%.2f", initialStockPrice));
                model.addAttribute("nextClosePrice", String.format("%.2f", nextClosePrice));
                model.addAttribute("varPercentage", String.format("%.2f", varPercentage));
                model.addAttribute("stockSymbol", stockSymbol);
            } catch (RuntimeException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "var/calculate-var";
    }

    @GetMapping("/")
    public String getDailyData(@RequestParam(required = false) String symbol,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate,
                               Model model) {
        try {
            if (symbol != null && !symbol.isEmpty()) {
                // Handle if both startDate and endDate are provided
                if (startDate != null && endDate != null) {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    Timestamp startTimestamp = Timestamp.valueOf(start.atStartOfDay());
                    Timestamp endTimestamp = Timestamp.valueOf(end.plusDays(1).atStartOfDay());
                    List<DailyData> dailyDataList = customDailyDataRepository.getByDateRangeAndSymbol(symbol, startTimestamp, endTimestamp);
                    model.addAttribute("dailyData", dailyDataList);
                } else if (startDate != null) {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.now();
                    Timestamp startTimestamp = Timestamp.valueOf(start.atStartOfDay());
                    Timestamp endTimestamp = Timestamp.valueOf(end.plusDays(1).atStartOfDay());
                    List<DailyData> dailyDataList = customDailyDataRepository.getByDateRangeAndSymbol(symbol, startTimestamp, endTimestamp);
                    model.addAttribute("dailyData", dailyDataList);
                } else {
                    DailyData dailyData = customDailyDataRepository.getBySymbol(symbol);
                    model.addAttribute("dailyData", dailyData);
                }
            } else {
                if (startDate != null && endDate != null) {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    Timestamp startTimestamp = Timestamp.valueOf(start.atStartOfDay());
                    Timestamp endTimestamp = Timestamp.valueOf(end.plusDays(1).atStartOfDay());
                    List<DailyData> dailyDataList = customDailyDataRepository.getByDateRangeAndSymbol(null, startTimestamp, endTimestamp);
                    model.addAttribute("dailyData", dailyDataList);
                } else {
                    List<DailyData> allData = customDailyDataRepository.getByDateRangeAndSymbol(null, null, null);
                    model.addAttribute("dailyData", allData);
                }
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "daily/dailydata";
    }

    // Route for multiple stock VaR calculation (investment route)
    @GetMapping("/investment")
    public String investmentPage(Model model) {
        // Optionally, load default data or setup necessary attributes for the investment page
        return "daily/investment";
    }

    @PostMapping("/investment/calculate")
    public ResponseEntity<Map<String, Object>> calculateMultipleVaR(
            @RequestBody Map<String, Double> frontendInvestmentData,
            @RequestParam int daysOfInvestment) {

        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = varCalculationService.calculateMultipleVaRForAllStocks(frontendInvestmentData, daysOfInvestment);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private boolean isPriceStable(String stockSymbol, double initialStockPrice) {
        List<Double> priceHistory = dailyDataService.getStockPriceHistory(stockSymbol, 2);
        if (priceHistory == null || priceHistory.size() < 2) {
            return false;
        }
        for (double price : priceHistory) {
            if (price != initialStockPrice) {
                return false;
            }
        }
        return true;
    }
    private double calculateNextClosePrice(String stockSymbol, double initialStockPrice) {
        LocalDate today = LocalDate.now();

        if (isPriceStable(stockSymbol, initialStockPrice)) {
            if (previousPredictions.containsKey(stockSymbol)) {
                logger.info("Price stable for {}. Returning previous prediction.", stockSymbol);
                return previousPredictions.get(stockSymbol);
            }
        }

        double percentageChange;

        if (initialStockPrice < 200) {
            percentageChange = (Math.random() * 8) - 4;
        } else if (initialStockPrice < 800) {
            percentageChange = (Math.random() * 12) - 6;
        } else if (initialStockPrice < 1500) {
            percentageChange = (Math.random() * 14) - 7;
        } else {
            percentageChange = (Math.random() * 18) - 9;
        }

        double nextClosePrice = initialStockPrice + (initialStockPrice * percentageChange / 100);
        previousPredictions.put(stockSymbol, nextClosePrice);

        return nextClosePrice;
    }
}
