package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.repository.nepse.DailyDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DailyDataService {

    @Autowired
    private DailyDataRepository dailyDataRepository;

    public List<DailyData> getAllDailyData() {
        return dailyDataRepository.findAll();
    }

    public Optional<DailyData> getDailyDataById(int id) {
        return dailyDataRepository.findById(id);
    }

    public DailyData saveDailyData(DailyData dailyData) {
        return dailyDataRepository.save(dailyData);
    }

    public void deleteDailyData(int id) {
        dailyDataRepository.deleteById(id);
    }
}