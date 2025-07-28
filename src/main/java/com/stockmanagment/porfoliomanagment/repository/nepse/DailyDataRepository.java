package com.stockmanagment.porfoliomanagment.repository.nepse;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;

public interface DailyDataRepository extends JpaRepository<DailyData, Integer> {

    DailyData findByDateAndSymbol(Timestamp timestamp, String symbol);
    @Query("SELECT d.close FROM DailyData d WHERE d.symbol = :stockSymbol ORDER BY d.date DESC LIMIT :days")
    List<Double> findPricesForLastNDays(@Param("stockSymbol") String stockSymbol, @Param("days") int days);

}
