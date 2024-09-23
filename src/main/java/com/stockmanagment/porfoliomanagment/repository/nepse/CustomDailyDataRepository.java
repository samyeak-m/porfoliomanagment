package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

public interface CustomDailyDataRepository {

    List<DailyData> getByDateRangeAndSymbol(String symbol, Timestamp startDate, Timestamp endDate);

    List<String> getAllSymbolsFromDailyData();

    DailyData getBySymbol(String symbol);
    List<DailyData> getByDate();

}
