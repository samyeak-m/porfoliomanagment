package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.HistockData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface HistockDataRepository extends JpaRepository<HistockData, Integer> {

    @Query("SELECT DISTINCT h.symbol FROM HistockData h")
    List<String> findDistinctSymbols();
}
