package com.stockmanagment.porfoliomanagment.service.nepse;

import jakarta.annotation.PostConstruct;
import com.stockmanagment.porfoliomanagment.repository.nepse.HistockDataRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TableCreationService {
    @PostConstruct
    public void init() {
        createTablesForStockSymbols();
    }

    private final HistockDataRepository histockDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public TableCreationService(HistockDataRepository histockDataRepository) {
        this.histockDataRepository = histockDataRepository;
    }

    @Transactional
    public void createTablesForStockSymbols() {
        List<String> distinctSymbols = histockDataRepository.findDistinctSymbols();
        for (String symbol : distinctSymbols) {
            String tableName = "daily_data_" + symbol.toLowerCase();
            createTable(tableName);
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
        entityManager.createNativeQuery(createTableQuery).executeUpdate();
    }
}
