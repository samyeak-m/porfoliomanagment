package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.Predictions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionsRepository extends JpaRepository<Predictions, Integer> { }