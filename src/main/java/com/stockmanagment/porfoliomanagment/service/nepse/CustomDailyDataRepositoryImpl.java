package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.repository.nepse.CustomDailyDataRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomDailyDataRepositoryImpl implements CustomDailyDataRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<DailyData> findByDate(String symbol, Timestamp date) {
        return List.of();
    }

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
            dailyData.setDate(Timestamp.valueOf(sqlDate.toLocalDate().atStartOfDay()));  // or convert to Timestamp if required

            dailyData.setOpen(BigDecimal.valueOf((Double) row[1]));
            dailyData.setHigh(BigDecimal.valueOf((Double) row[2]));
            dailyData.setLow(BigDecimal.valueOf((Double) row[3]));
            dailyData.setClose(BigDecimal.valueOf((Double) row[4]));

            return dailyData;
        } else {
            return null;
        }
    }

    @Override
    public List<DailyData> getByDateRangeAndSymbol(String symbol, Timestamp startDate, Timestamp endDate) {
        String tableName = "daily_data_" + symbol;
        String queryStr = "SELECT date, open, high, low, close FROM " + tableName + " WHERE date BETWEEN :startDate AND :endDate";
        Query query = entityManager.createNativeQuery(queryStr);

        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        List<Object[]> resultList = query.getResultList();
        List<DailyData> dailyDataList = new ArrayList<>();

        for (Object[] row : resultList) {
            // Manually map each row to a DailyData object
            DailyData dailyData = new DailyData();

            // Use java.sql.Date for date and convert if needed
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            dailyData.setDate(Timestamp.valueOf(sqlDate.toLocalDate().atStartOfDay()));  // or convert to Timestamp if required

            dailyData.setOpen(BigDecimal.valueOf((Double) row[1]));
            dailyData.setHigh(BigDecimal.valueOf((Double) row[2]));
            dailyData.setLow(BigDecimal.valueOf((Double) row[3]));
            dailyData.setClose(BigDecimal.valueOf((Double) row[4]));

            dailyDataList.add(dailyData);
        }

        return dailyDataList;
    }

    @Override
    public List<DailyData> getAllData() {
        String queryStr = "SELECT * FROM daily_data ORDER BY date DESC";
        Query query = entityManager.createNativeQuery(queryStr, DailyData.class);
        return query.getResultList();
    }

    @Override
    public List<String> getAllSymbols() {
        String queryStr = "SELECT DISTINCT symbol FROM daily_data";
        Query query = entityManager.createNativeQuery(queryStr);
        return query.getResultList();
    }


}
