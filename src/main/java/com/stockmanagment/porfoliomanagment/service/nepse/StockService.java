package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.Stock;
import com.stockmanagment.porfoliomanagment.repository.nepse.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final StockRepository stockRepository;

    @Autowired
    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    // Method to fetch investment amounts for all stocks
    public Map<String, Double> getStockInvestmentAmounts() {
        return stockRepository.findAll().stream()  // Use findAll() to get all stocks
                .collect(Collectors.toMap(Stock::getSymbol, Stock::getInvestmentAmount));
    }

    // Method to fetch stock price by symbol
    public Optional<Double> getStockPrice(String stockSymbol) {
        Optional<Stock> stock = stockRepository.findBySymbol(stockSymbol);
        return stock.map(Stock::getPrice);  // Assuming Stock entity has a getPrice method
    }

    // Method to fetch close prices for a stock (for example, over the last 30 days)
    public List<Double> getStockClosePrices(String stockSymbol) {
        Optional<Stock> stock = stockRepository.findBySymbol(stockSymbol);
        return stock.map(Stock::getClosePrices)  // Assuming Stock entity has a getClosePrices method
                .orElseThrow(() -> new RuntimeException("No close prices available for stock: " + stockSymbol));
    }

    // Method to fetch historical stock prices (e.g., for calculating VaR)
    public List<Double> getHistoricalPrices(String stockSymbol) {
        Optional<Stock> stock = stockRepository.findBySymbol(stockSymbol);
        return stock.map(Stock::getHistoricalPrices)  // Assuming Stock entity has a method to fetch historical prices
                .orElseThrow(() -> new RuntimeException("No historical prices available for stock: " + stockSymbol));
    }
}
