package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.YearlyData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface YearlyDataRepository extends JpaRepository<YearlyData, Integer> { }