package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

public interface DailyDataRepository extends JpaRepository<DailyData, Integer> {

    DailyData findByDateAndSymbol(Timestamp timestamp, String symbol);
    @Query("SELECT d.close FROM DailyData d WHERE d.symbol = :stockSymbol ORDER BY d.date DESC LIMIT :days")
    List<Double> findPricesForLastNDays(@Param("stockSymbol") String stockSymbol, @Param("days") int days);

}
