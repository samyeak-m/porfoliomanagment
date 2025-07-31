package com.stockmanagment.porfoliomanagment.service;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.stockmanagment.porfoliomanagment.service.nepse.lstm.database.DatabaseHelper;

@Service
@Component
public class StockSymbolCacheService {
    
    private final DatabaseHelper databaseHelper;
    
    @Autowired
    public StockSymbolCacheService(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }
    
    @Cacheable(value = "stockSymbols", key = "'all'")
    public List<String> getCachedStockSymbols() {
        try {
            return databaseHelper.getAllStockTableNames()
                    .stream()
                    .map(String::toUpperCase)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch stock symbols", e);
        }
    }
    
    @CacheEvict(value = "stockSymbols", allEntries = true)
    public void clearCache() {
        // Cache will be cleared
    }
}