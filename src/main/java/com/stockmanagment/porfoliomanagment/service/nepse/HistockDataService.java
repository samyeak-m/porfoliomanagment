package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.HistockData;
import com.stockmanagment.porfoliomanagment.repository.nepse.HistockDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HistockDataService {

    @Autowired
    private HistockDataRepository histockDataRepository;

    public List<HistockData> getAllHistockData() {
        return histockDataRepository.findAll();
    }

    public Optional<HistockData> getHistockDataById(int id) {
        return histockDataRepository.findById(id);
    }

    public HistockData saveHistockData(HistockData histockData) {
        return histockDataRepository.save(histockData);
    }

    public void deleteHistockData(int id) {
        histockDataRepository.deleteById(id);
    }
}