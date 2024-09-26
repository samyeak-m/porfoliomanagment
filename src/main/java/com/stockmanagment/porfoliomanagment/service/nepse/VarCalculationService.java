package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.VarData;
import com.stockmanagment.porfoliomanagment.model.nepse.VarOfAllData;
import com.stockmanagment.porfoliomanagment.repository.nepse.VarDataRepository;
import com.stockmanagment.porfoliomanagment.repository.nepse.VarOfAllDataRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VarCalculationService {

    private static final int NUM_SIMULATIONS = 10000;
    private final VarDataRepository varDataRepository;
    private final VarOfAllDataRepository varOfAllDataRepository;
    private final StockService stockService;

    @PersistenceContext(unitName = "nepse")
    private EntityManager entityManager;

    @Autowired
    public VarCalculationService(VarDataRepository varDataRepository,
                                 VarOfAllDataRepository varOfAllDataRepository,
                                 StockService stockService) {
        this.varDataRepository = varDataRepository;
        this.varOfAllDataRepository = varOfAllDataRepository;
        this.stockService = stockService;
    }

    @PostConstruct
    public void initialize() {
        Map<String, Double> frontendInvestmentAmounts = new HashMap<>(); // Initialize with actual data if available
        Map<String, Double> stockInvestmentMap = getStockInvestmentAmounts(frontendInvestmentAmounts); // Pass the parameter
        // Retrieve stocks and their investment amounts

            // Filter stocks where (investment amount / stock price) <= (stock price * 30)
            List<String> eligibleStocks = stockInvestmentMap.entrySet().stream()
                    .filter(entry -> {
                        String stockSymbol = entry.getKey();
                        double investmentAmount = entry.getValue();
                        double initialStockPrice = getInitialStockPrice(stockSymbol);
                        return (investmentAmount / initialStockPrice) <= (initialStockPrice * 30);
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            Random random = new Random();
            for (String stockSymbol : eligibleStocks) {
                int randomDays = 10 + random.nextInt(250);
                double randomConfidenceLevel = 0.90 + (0.09 * random.nextDouble());
                calculateAndStoreVaR(stockSymbol, randomDays, randomConfidenceLevel, true);

        }
    }

    public boolean isTableEmpty() {
        return varOfAllDataRepository.count() == 0;
    }

    public List<String> getAllStockSymbols() {
        return varOfAllDataRepository.findAll()
                .stream()
                .map(VarOfAllData::getStockSymbol)
                .distinct()
                .collect(Collectors.toList());
    }

    public void calculateAndStoreVaR(String stockSymbol, int daysOfInvestment, double confidenceLevel, boolean forAll) {
        List<Double> closePrices = getClosePrices(stockSymbol);

        if (closePrices.isEmpty()) {
            throw new RuntimeException("No data available for the given stock symbol: " + stockSymbol);
        }

        if (closePrices.size() < daysOfInvestment) {
            daysOfInvestment = closePrices.size();
        }

        double initialStockPrice = closePrices.get(closePrices.size() - 1);
        double meanReturn = calculateMeanReturn(closePrices, daysOfInvestment);
        double volatility = calculateVolatility(closePrices, daysOfInvestment, meanReturn);
        double var = calculateVaR(initialStockPrice, meanReturn, volatility, daysOfInvestment, NUM_SIMULATIONS, confidenceLevel);

        if (forAll) {
            storeVarOfAllData(stockSymbol, daysOfInvestment, meanReturn, volatility, var, initialStockPrice, confidenceLevel);
        } else {
            storeVaRData(stockSymbol, daysOfInvestment, meanReturn, volatility, var, initialStockPrice, confidenceLevel);
        }
    }

    public List<Double> getClosePrices(String stockSymbol) {
        String tableName = "daily_data_" + stockSymbol.toLowerCase();
        String query = "SELECT close FROM " + tableName + " ORDER BY date ASC";
        return (List<Double>) entityManager.createNativeQuery(query)
                .getResultList()
                .stream()
                .map(result -> ((Number) result).doubleValue())
                .collect(Collectors.toList());
    }

    public double calculateMeanReturn(List<Double> prices, int days) {
        if (prices.size() < 2) {
            throw new IllegalArgumentException("Insufficient data to calculate mean return.");
        }

        double totalReturn = 0.0;
        for (int i = 1; i < days && i < prices.size(); i++) {
            totalReturn += (prices.get(i) - prices.get(i - 1)) / prices.get(i - 1);
        }
        return totalReturn / Math.min(days, prices.size());
    }

    public double calculateVolatility(List<Double> prices, int days, double meanReturn) {
        double sumOfSquares = 0.0;
        for (int i = 1; i < days && i < prices.size(); i++) {
            double dailyReturn = (prices.get(i) - prices.get(i - 1)) / prices.get(i - 1);
            sumOfSquares += Math.pow(dailyReturn - meanReturn, 2);
        }
        return Math.sqrt(sumOfSquares / (days - 1));
    }

    public double calculateVaR(double initialStockPrice, double meanReturn, double volatility, int daysOfInvestment, int numSimulations, double confidenceLevel) {
        Random random = new Random();
        double[] simulatedEndPrices = new double[numSimulations];

        for (int i = 0; i < numSimulations; i++) {
            double simulatedPrice = initialStockPrice;
            for (int j = 0; j < daysOfInvestment; j++) {
                double randomShock = random.nextGaussian();
                simulatedPrice += simulatedPrice * (meanReturn + volatility * randomShock);
            }
            simulatedEndPrices[i] = simulatedPrice;
        }

        double varThreshold = 1 - confidenceLevel;
        int varIndex = (int) (varThreshold * numSimulations);
        java.util.Arrays.sort(simulatedEndPrices);
        return initialStockPrice - simulatedEndPrices[varIndex];
    }

    private void storeVaRData(String stockSymbol, int daysOfInvestment, double meanReturn, double volatility, double var, double initialStockPrice, double confidenceLevel) {
        VarData varData = new VarData();
        varData.setStockSymbol(stockSymbol);
        varData.setDaysOfInvestment(daysOfInvestment);
        varData.setMeanReturn(meanReturn);
        varData.setVolatility(volatility);
        varData.setVar(var);
        varData.setInitialStockPrice(initialStockPrice);
        varData.setConfidenceLevel(confidenceLevel);
        varDataRepository.save(varData);
    }

    private void storeVarOfAllData(String stockSymbol, int daysOfInvestment, double meanReturn, double volatility, double var, double initialStockPrice, double confidenceLevel) {
        VarOfAllData varOfAllData = new VarOfAllData();
        varOfAllData.setStockSymbol(stockSymbol);
        varOfAllData.setDaysOfInvestment(daysOfInvestment);
        varOfAllData.setMeanReturn(meanReturn);
        varOfAllData.setVolatility(volatility);
        varOfAllData.setVar(var);
        varOfAllData.setInitialStockPrice(initialStockPrice);
        varOfAllData.setConfidenceLevel(confidenceLevel);
        varOfAllDataRepository.save(varOfAllData);
    }

    public double calculateDynamicConfidenceLevel(String stockSymbol) {
        List<Double> confidenceLevels = varOfAllDataRepository.findTop30ByStockSymbolOrderByDateDesc(stockSymbol)
                .stream()
                .map(VarOfAllData::getConfidenceLevel)
                .collect(Collectors.toList());

        return confidenceLevels.stream().mapToDouble(Double::doubleValue).average().orElse(0.95);
    }

    public double calculateVaR(String stockSymbol, int daysOfInvestment, double confidenceLevel) {
        List<Double> closePrices = getClosePrices(stockSymbol);

        if (closePrices.isEmpty()) {
            throw new RuntimeException("No data available for the given stock symbol: " + stockSymbol);
        }

        if (closePrices.size() < daysOfInvestment) {
            daysOfInvestment = closePrices.size();
        }

        double initialStockPrice = closePrices.get(closePrices.size() - 1);
        double meanReturn = calculateMeanReturn(closePrices, daysOfInvestment);
        double volatility = calculateVolatility(closePrices, daysOfInvestment, meanReturn);
        return calculateVaR(initialStockPrice, meanReturn, volatility, daysOfInvestment, NUM_SIMULATIONS, confidenceLevel);
    }

    public Map<String, Double> getStockInvestmentAmounts(Map<String, Double> frontendInvestmentAmounts) {
        Map<String, Double> filteredStockInvestmentMap = new HashMap<>();

        for (Map.Entry<String, Double> entry : frontendInvestmentAmounts.entrySet()) {
            String stockSymbol = entry.getKey();
            double investmentAmount = entry.getValue();
            Optional<Double> stockPriceOpt = stockService.getStockPrice(stockSymbol);

            if (stockPriceOpt.isPresent()) {
                double stockPrice = stockPriceOpt.get();

                // This is where the filtering logic is applied
                if (investmentAmount / stockPrice <= stockPrice * 30) {
                    filteredStockInvestmentMap.put(stockSymbol, investmentAmount);
                }
            }
        }

        return filteredStockInvestmentMap;
    }

    // New method to get initial stock price
    public double getInitialStockPrice(String stockSymbol) {
        List<Double> closePrices = getClosePrices(stockSymbol);
        if (closePrices.isEmpty()) {
            throw new RuntimeException("No closing prices available for the given stock symbol: " + stockSymbol);
        }
        return closePrices.get(0); // Assuming the first price is the initial stock price
    }

    public Map<String, Object> calculateMultipleVaRForAllStocks(Map<String, Double> stockAndDaysMap, double confidenceLevel) {
        Map<String, Object> results = new HashMap<>();

        for (Map.Entry<String, Double> entry : stockAndDaysMap.entrySet()) {
            String stockSymbol = entry.getKey();
            double daysOfInvestment = entry.getValue();

            double var = calculateVaR(stockSymbol, (int) daysOfInvestment, confidenceLevel);
            Map<String, Object> result = new HashMap<>();
            result.put("stockSymbol", stockSymbol);
            result.put("varValue", var);
            results.put(stockSymbol, result);
        }

        return results;
    }

    public List<String> getStocksBasedOnInvestmentCriteria() {
        return varDataRepository.findUniqueStocksBasedOnInvestmentCriteria();
    }

    public Map<String, Object> calculateMultipleVaRBasedOnInvestmentCriteria(int daysOfInvestment, double defaultConfidenceLevel) {
        Map<String, Object> results = new HashMap<>();
        List<String> eligibleStocks = getStocksBasedOnInvestmentCriteria();  // Get stocks that meet the investment criteria

        for (String stockSymbol : eligibleStocks) {
            double confidenceLevel = calculateDynamicConfidenceLevel(stockSymbol); // Calculate dynamic confidence level
            double var = calculateVaR(stockSymbol, daysOfInvestment, confidenceLevel);
            double initialPrice = getInitialStockPrice(stockSymbol);

            Map<String, Object> stockData = new HashMap<>();
            stockData.put("stockSymbol", stockSymbol);
            stockData.put("varValue", var);
            stockData.put("confidenceLevel", confidenceLevel * 100);
            stockData.put("initialPrice", initialPrice);

            results.put(stockSymbol, stockData);
        }

        return results;
    }


}
