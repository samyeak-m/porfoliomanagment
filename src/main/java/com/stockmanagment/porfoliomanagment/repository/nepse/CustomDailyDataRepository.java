package com.stockmanagment.porfoliomanagment.repository.nepse;

import java.sql.Timestamp;
import java.util.List;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;

public interface CustomDailyDataRepository {

    List<DailyData> getByDateRangeAndSymbol(String symbol, Timestamp startDate, Timestamp endDate);

    List<String> getAllSymbolsFromDailyData();

    DailyData getBySymbol(String symbol);
    List<DailyData> getByDate();

}
