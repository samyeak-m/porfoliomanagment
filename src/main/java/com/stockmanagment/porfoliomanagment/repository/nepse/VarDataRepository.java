package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.VarData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VarDataRepository extends JpaRepository<VarData, Integer> {
    List<VarData> findByStockSymbolOrderByDateAsc(String stockSymbol);

    @Query(value = "SELECT DISTINCT v.stock_symbol FROM var_data v WHERE (investment_amount / stock_price) <= (stock_price * 30)", nativeQuery = true)
    List<String> findUniqueStocksBasedOnInvestmentCriteria();

    // New method to find the latest VarData entry for today
//    @Query(value = "SELECT * FROM var_data v WHERE DATE(v.date) = CURDATE() ORDER BY v.date DESC LIMIT 1", nativeQuery = true)
//    VarData findLatestTodayData();
}
