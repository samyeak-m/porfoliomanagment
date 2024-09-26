package com.stockmanagment.porfoliomanagment.controller.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.Predictions;
import com.stockmanagment.porfoliomanagment.service.nepse.PredictionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/predictions")
public class PredictionsController {

    @Autowired
    private PredictionsService predictionsService;

    @GetMapping
    public List<Predictions> getAllPredictions() {
        return predictionsService.getAllPredictions();
    }

    @GetMapping("/{id}")
    public Optional<Predictions> getPredictionsById(@PathVariable int id) {
        return predictionsService.getPredictionsById(id);
    }

    @PostMapping
    public Predictions createPredictions(@RequestBody Predictions predictions) {
        return predictionsService.savePredictions(predictions);
    }

    @PutMapping("/{id}")
    public Predictions updatePredictions(@PathVariable int id, @RequestBody Predictions predictions) {
        predictions.setId(id);
        return predictionsService.savePredictions(predictions);
    }

    @DeleteMapping("/{id}")
    public void deletePredictions(@PathVariable int id) {
        predictionsService.deletePredictions(id);
    }
}