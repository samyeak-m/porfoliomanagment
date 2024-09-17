package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.VarData;
import com.stockmanagment.porfoliomanagment.model.nepse.VarOfAllData;
import com.stockmanagment.porfoliomanagment.repository.nepse.VarDataRepository;
import com.stockmanagment.porfoliomanagment.repository.nepse.VarOfAllDataRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class VarCalculationService {

    private static final int NUM_SIMULATIONS = 10000;
    private final VarDataRepository varDataRepository;
    private final VarOfAllDataRepository varOfAllDataRepository;

    @PersistenceContext(unitName = "nepse")
    private EntityManager entityManager;


    public VarCalculationService(VarDataRepository varDataRepository, VarOfAllDataRepository varOfAllDataRepository) {
        this.varDataRepository = varDataRepository;
        this.varOfAllDataRepository = varOfAllDataRepository;
    }

    @PostConstruct
    public void initialize() {
        if (isTableEmpty()) {
            List<String> stockSymbols = getAllStockSymbols();
            Random random = new Random();

            for (String stockSymbol : stockSymbols) {
                int randomDays = 10 + random.nextInt(250);
                double randomConfidenceLevel = 0.90 + (0.09 * random.nextDouble());
                calculateAndStoreVaR(stockSymbol, randomDays, randomConfidenceLevel, true);
            }
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

    public double getInitialStockPrice(String stockSymbol){
        List<Double> closePrices = getClosePrices(stockSymbol);
        double initialStockPrice = closePrices.get(closePrices.size() - 1);
        return initialStockPrice;

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
        double totalReturn = 0.0;
        for (int i = 1; i < days; i++) {
            totalReturn += (prices.get(i) - prices.get(i - 1)) / prices.get(i - 1);
        }
        return totalReturn / days;
    }

    public double calculateVolatility(List<Double> prices, int days, double meanReturn) {
        double sumOfSquares = 0.0;
        for (int i = 1; i < days; i++) {
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
}
