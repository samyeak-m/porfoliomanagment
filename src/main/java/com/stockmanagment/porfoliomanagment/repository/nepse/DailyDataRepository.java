package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

public interface DailyDataRepository extends JpaRepository<DailyData, Integer> {

    DailyData findByDateAndSymbol(Timestamp timestamp, String symbol);
}
