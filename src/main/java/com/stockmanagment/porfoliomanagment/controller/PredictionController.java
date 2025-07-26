package com.stockmanagment.porfoliomanagment.controller;

import com.stockmanagment.porfoliomanagment.dto.PredictionRequestDTO;
import com.stockmanagment.porfoliomanagment.dto.PredictionResponseDTO;
import com.stockmanagment.porfoliomanagment.service.LstmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lstm")
public class PredictionController {

    @Autowired
    private LstmService lstmService;

    @PostMapping("/predict")
    public PredictionResponseDTO predict(@RequestBody PredictionRequestDTO request) {
        return lstmService.predict(request.getStockSymbol());
    }

    @PostMapping("/train")
    public String train() {
        lstmService.train();
        return "Training started!";
    }
}
