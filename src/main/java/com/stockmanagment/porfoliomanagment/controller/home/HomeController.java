package com.stockmanagment.porfoliomanagment.controller.home;

import com.stockmanagment.porfoliomanagment.model.portfolio.UserDetail;
import com.stockmanagment.porfoliomanagment.service.portfolio.UserDetailService;
import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.repository.nepse.CustomDailyDataRepository;
import com.stockmanagment.porfoliomanagment.service.nepse.DailyDataService;
import com.stockmanagment.porfoliomanagment.service.nepse.VarCalculationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final VarCalculationService varCalculationService;
    private final DailyDataService dailyDataService;
    private final CustomDailyDataRepository customDailyDataRepository;
    private final UserDetailService userDetailService;

    public HomeController(VarCalculationService varCalculationService, DailyDataService dailyDataService,
                          CustomDailyDataRepository customDailyDataRepository, UserDetailService userDetailService) {
        this.varCalculationService = varCalculationService;
        this.dailyDataService = dailyDataService;
        this.customDailyDataRepository = customDailyDataRepository;
        this.userDetailService = userDetailService;
    }

    @GetMapping("/")
    public String index() {
        return "index"; // Home page
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        Optional<UserDetail> userDetail = userDetailService.login(email, password);
        if (userDetail.isPresent()) {
            session.setAttribute("user", userDetail.get());
            return "redirect:/index"; // Redirect to home page
        }
        model.addAttribute("error", "Invalid email or password");
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "auth/signup"; // Return the signup page view
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String email,
                         @RequestParam String password,
                         @RequestParam String phone, // Add phone parameter
                         Model model) {
        UserDetail newUser = new UserDetail();
        newUser.setEmail(email);
        newUser.setPassword(password); // Consider hashing the password
        newUser.setPhone(phone); // Set the phone number

        userDetailService.saveUserDetail(newUser);

        model.addAttribute("message", "Signup successful! Please login.");
        return "auth/login";
    }



    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login"; // Redirect to login page
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
}
