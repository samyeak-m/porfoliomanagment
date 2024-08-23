package com.stockmanagment.porfoliomanagment.repository.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.LiveData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveDataRepository extends JpaRepository<LiveData, Integer> { }