package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.VarOfAllData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VarOfAllDataRepository extends JpaRepository<VarOfAllData, Integer> {
    List<VarOfAllData> findTop30ByStockSymbolOrderByDateDesc(String stockSymbol);
}
