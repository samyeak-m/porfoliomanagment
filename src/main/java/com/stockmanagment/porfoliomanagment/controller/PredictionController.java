package com.stockmanagment.porfoliomanagment.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockmanagment.porfoliomanagment.dto.PredictionRequestDTO;
import com.stockmanagment.porfoliomanagment.dto.PredictionResponseDTO;
import com.stockmanagment.porfoliomanagment.service.LstmService;

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

    @GetMapping("/metrics")
    public String getMetrics() throws IOException {
        String path = "src/main/resources/static/model/output_v1_e10_b16_h20/confusion.txt";
        return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
    }
}
