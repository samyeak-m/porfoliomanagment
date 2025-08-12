package com.stockmanagment.porfoliomanagment.repository.nepse;

import java.sql.Timestamp;
import java.util.List;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;

/**
 * Repository abstraction implemented by CustomDailyDataRepositoryImpl.
 */
public interface CustomDailyDataRepository {

    DailyData getBySymbol(String symbol);

    List<DailyData> getByDateRangeAndSymbol(String symbol, Timestamp startDate, Timestamp endDate);

    List<DailyData> getByDate();

    List<String> getAllSymbolsFromDailyData();

    DailyData getLatestBySymbol(String symbol);
}
