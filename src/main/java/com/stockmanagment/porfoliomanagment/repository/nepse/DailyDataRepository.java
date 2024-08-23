package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyDataRepository extends JpaRepository<DailyData, Integer> { }
