package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface CustomDailyDataRepository {

    @Query("SELECT d FROM daily_data_#{#symbol} d WHERE d.date BETWEEN :startDate AND :endDate")
    List<DailyData> getByDateRangeAndSymbol(@Param("symbol") String symbol, @Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate);

    DailyData getBySymbol(String symbol);
    List<DailyData> getAllData(); // To fetch all data
    List<String> getAllSymbols();


}
