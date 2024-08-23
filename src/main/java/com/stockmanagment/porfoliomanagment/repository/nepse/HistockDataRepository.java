package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.HistockData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistockDataRepository extends JpaRepository<HistockData, Integer> { }