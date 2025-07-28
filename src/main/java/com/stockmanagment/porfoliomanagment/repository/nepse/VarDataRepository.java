package com.stockmanagment.porfoliomanagment.repository.nepse;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.stockmanagment.porfoliomanagment.model.nepse.VarData;

public interface VarDataRepository extends JpaRepository<VarData, Integer> {
    List<VarData> findByStockSymbolOrderByDateAsc(String stockSymbol);

    @Query(value = "SELECT DISTINCT v.stock_symbol FROM var_data v WHERE (investment_amount / stock_price) <= (stock_price * 30)", nativeQuery = true)
    List<String> findUniqueStocksBasedOnInvestmentCriteria();

//    @Query(value = "SELECT * FROM var_data v WHERE DATE(v.date) = CURDATE() ORDER BY v.date DESC LIMIT 1", nativeQuery = true)
//    VarData findLatestTodayData();
}
