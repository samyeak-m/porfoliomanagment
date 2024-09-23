package com.stockmanagment.porfoliomanagment.service.nepse;

import jakarta.annotation.PostConstruct;
import com.stockmanagment.porfoliomanagment.repository.nepse.HistockDataRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TableCreationService {

    private static final Logger logger = LoggerFactory.getLogger(TableCreationService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final HistockDataRepository histockDataRepository;

    public TableCreationService(HistockDataRepository histockDataRepository) {
        this.histockDataRepository = histockDataRepository;
    }

    @PostConstruct
    public void init() {
        createTablesForStockSymbols();
    }

    @Transactional
    public void createTablesForStockSymbols() {
        List<String> distinctSymbols = histockDataRepository.findDistinctSymbols();
        for (String symbol : distinctSymbols) {
            String tableName = "daily_data_" + symbol.toLowerCase();
            try {
                logger.info("Creating table for symbol: {}", symbol);
                createTable(tableName);
            } catch (Exception e) {
                logger.error("Error creating table for symbol: " + symbol, e);
            }
        }
    }

    private void createTable(String tableName) {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "date DATE," +
                "open DOUBLE," +
                "high DOUBLE," +
                "low DOUBLE," +
                "close DOUBLE," +
                "PRIMARY KEY (date)" +
                ")";
        try {
            entityManager.createNativeQuery(createTableQuery).executeUpdate();
            logger.info("Table '{}' created successfully", tableName);
        } catch (Exception e) {
            logger.error("Error executing create table query for table: {}", tableName, e);
            throw e;  // Rethrow the exception to ensure transaction rollback if needed
        }
    }
}
