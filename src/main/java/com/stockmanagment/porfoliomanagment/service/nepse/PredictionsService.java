// src/main/java/com/stockmanagment/porfoliomanagment/service/nepse/PredictionsService.java
package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.Predictions;
import com.stockmanagment.porfoliomanagment.repository.nepse.PredictionsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PredictionsService {

    @Autowired
    private PredictionsRepository predictionsRepository;

    public List<Predictions> getAllPredictions() {
        return predictionsRepository.findAll();
    }

    public Optional<Predictions> getPredictionsById(int id) {
        return predictionsRepository.findById(id);
    }

    public Predictions savePredictions(Predictions predictions) {
        return predictionsRepository.save(predictions);
    }

    public void deletePredictions(int id) {
        predictionsRepository.deleteById(id);
    }
}
