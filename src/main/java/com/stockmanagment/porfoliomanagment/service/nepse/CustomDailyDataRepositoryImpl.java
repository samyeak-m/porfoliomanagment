package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.repository.nepse.CustomDailyDataRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomDailyDataRepositoryImpl implements CustomDailyDataRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DailyData getBySymbol(String symbol) {
        String tableName = "daily_data_" + symbol;
        String queryStr = "SELECT date, open, high, low, close FROM " + tableName + " ORDER BY date DESC LIMIT 1";
        Query query = entityManager.createNativeQuery(queryStr);

        List<Object[]> resultList = query.getResultList();

        if (!resultList.isEmpty()) {
            Object[] row = resultList.get(0);

            // Manually map the row data to DailyData
            DailyData dailyData = new DailyData();

            // Use java.sql.Date for date and convert if needed
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            LocalDate localDate = sqlDate.toLocalDate();
            dailyData.setDate(localDate);

            dailyData.setOpen(Double.valueOf((Double) row[1]));
            dailyData.setHigh(Double.valueOf((Double) row[2]));
            dailyData.setLow(Double.valueOf((Double) row[3]));
            dailyData.setClose(Double.valueOf((Double) row[4]));

            return dailyData;
        } else {
            return null;
        }
    }

    @Override
    public List<DailyData> getByDateRangeAndSymbol(String symbol, Timestamp startDate, Timestamp endDate) {
        // Use the default table if symbol is null or empty
        String tableName = (symbol == null || symbol.isEmpty()) ? "daily_data" : "daily_data_" + symbol;

        String queryStr = "SELECT date, open, high, low, close FROM " + tableName + " WHERE date BETWEEN :startDate AND :endDate";

        Query query = entityManager.createNativeQuery(queryStr);

        // Set query parameters
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        // Execute the query and map the result
        List<Object[]> resultList = query.getResultList();
        List<DailyData> dailyDataList = new ArrayList<>();

        for (Object[] row : resultList) {
            DailyData dailyData = new DailyData();
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            LocalDate localDate = sqlDate.toLocalDate(); // Convert to LocalDate
            dailyData.setDate(localDate);
            dailyData.setOpen(Double.valueOf((Double) row[1]));
            dailyData.setHigh(Double.valueOf((Double) row[2]));
            dailyData.setLow(Double.valueOf((Double) row[3]));
            dailyData.setClose(Double.valueOf((Double) row[4]));

            dailyDataList.add(dailyData);
        }

        return dailyDataList;
    }

    @Override
    public List<DailyData> getByDate() {
        // Updated query to exclude 'date' from the selection
        String queryStr = "SELECT date, open, high, low, close, symbol FROM daily_data";
        Query query = entityManager.createNativeQuery(queryStr);

        List<Object[]> resultList = query.getResultList();
        List<DailyData> dailyDataList = new ArrayList<>();

        for (Object[] row : resultList) {
            DailyData dailyData = new DailyData();
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            LocalDate localDate = sqlDate.toLocalDate(); // Convert to LocalDate
            dailyData.setDate(localDate);
            dailyData.setOpen(Double.valueOf(((Number) row[1]).doubleValue()));
            dailyData.setHigh(Double.valueOf(((Number) row[2]).doubleValue()));
            dailyData.setLow(Double.valueOf(((Number) row[3]).doubleValue()));
            dailyData.setClose(Double.valueOf(((Number) row[4]).doubleValue()));
            dailyData.setSymbol(row[5].toString());

            dailyDataList.add(dailyData);
        }

        return dailyDataList;
    }


    @Override
    public List<String> getAllSymbolsFromDailyData() {
        String queryStr = "SELECT DISTINCT symbol FROM daily_data";
        Query query = entityManager.createNativeQuery(queryStr);

        // Get a list of symbols
        List<String> symbols = query.getResultList();

        return symbols;
    }



}
