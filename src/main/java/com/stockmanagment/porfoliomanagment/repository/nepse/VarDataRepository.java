package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.VarData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VarDataRepository extends JpaRepository<VarData, Integer> {
    List<VarData> findByStockSymbolOrderByDateAsc(String stockSymbol);
}
