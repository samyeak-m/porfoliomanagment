package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.YearlyData;
import com.stockmanagment.porfoliomanagment.repository.nepse.YearlyDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class YearlyDataService {

    @Autowired
    private YearlyDataRepository yearlyDataRepository;

    public List<YearlyData> getAllYearlyData() {
        return yearlyDataRepository.findAll();
    }

    public Optional<YearlyData> getYearlyDataById(int id) {
        return yearlyDataRepository.findById(id);
    }

    public YearlyData saveYearlyData(YearlyData yearlyData) {
        return yearlyDataRepository.save(yearlyData);
    }

    public void deleteYearlyData(int id) {
        yearlyDataRepository.deleteById(id);
    }
}