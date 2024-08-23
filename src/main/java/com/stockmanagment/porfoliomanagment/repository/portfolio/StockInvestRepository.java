package com.stockmanagment.porfoliomanagment.repository.portfolio;

import com.stockmanagment.porfoliomanagment.model.portfolio.StockInvest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockInvestRepository extends JpaRepository<StockInvest, Integer> { }