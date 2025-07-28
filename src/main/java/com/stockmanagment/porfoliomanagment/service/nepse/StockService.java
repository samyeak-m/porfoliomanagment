package com.stockmanagment.porfoliomanagment.service.nepse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stockmanagment.porfoliomanagment.model.nepse.Stock;
import com.stockmanagment.porfoliomanagment.repository.nepse.StockRepository;

@Service
public class StockService {

    private final StockRepository stockRepository;

    @Autowired
    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public Map<String, Double> getStockInvestmentAmounts() {
        return stockRepository.findAll().stream()
                .collect(Collectors.toMap(Stock::getSymbol, Stock::getInvestmentAmount));
    }

    public Optional<Double> getStockPrice(String stockSymbol) {
        Optional<Stock> stock = stockRepository.findBySymbol(stockSymbol);
        return stock.map(Stock::getPrice);
    }

    public List<Double> getStockClosePrices(String stockSymbol) {
        Optional<Stock> stock = stockRepository.findBySymbol(stockSymbol);
        return stock.map(Stock::getClosePrices)
                .orElseThrow(() -> new RuntimeException("No close prices available for stock: " + stockSymbol));
    }

    public List<Double> getHistoricalPrices(String stockSymbol) {
        Optional<Stock> stock = stockRepository.findBySymbol(stockSymbol);
        return stock.map(Stock::getHistoricalPrices)
                .orElseThrow(() -> new RuntimeException("No historical prices available for stock: " + stockSymbol));
    }
}
