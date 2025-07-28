package com.stockmanagment.porfoliomanagment.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockmanagment.porfoliomanagment.config.LstmConfig;
import com.stockmanagment.porfoliomanagment.dto.PredictionRequestDTO;
import com.stockmanagment.porfoliomanagment.dto.PredictionResponseDTO;
import com.stockmanagment.porfoliomanagment.service.LstmService;

@RestController
@RequestMapping("/api/lstm")
public class PredictionController {

    @Autowired
    private LstmService lstmService;
    
    @Autowired
    private LstmConfig config; // Inject config

    @PostMapping("/predict")
    public PredictionResponseDTO predict(@RequestBody PredictionRequestDTO request) {
        return lstmService.predict(request.getStockSymbol());
    }

    @PostMapping("/train")
    public String train() {
        lstmService.train();
        return "Training started!";
    }

    @PostMapping("/incremental-train")
    public String incrementalTrain() {
        lstmService.incrementalLearning();
        return "Incremental training completed!";
    }

    @GetMapping("/metrics")
    public String getMetrics() throws IOException {
        // Use dynamic path from config
        String path = config.getOutputDir() + "/confusion.txt";
        return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
    }

    @GetMapping("/training-status")
    public Map<String, Object> getTrainingStatus() {
        return lstmService.getTrainingStatus();
    }

    @GetMapping("/health")
    public Map<String, Object> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("memoryUsage", getMemoryUsage());
        health.put("modelLoaded", isModelLoaded());
        return health;
    }

    @GetMapping("/model-info")
    public Map<String, Object> getModelInfo() {
        return lstmService.getModelInfo();
    }

    @PostMapping("/validate-model")
    public String validateModel() {
        return lstmService.validateModel();
    }

    @GetMapping("/training-progress")
    public Map<String, Object> getTrainingProgress() {
        return lstmService.getCurrentTrainingProgress();
    }

    private Map<String, Object> getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("total", runtime.totalMemory() / 1024 / 1024);
        memory.put("free", runtime.freeMemory() / 1024 / 1024);
        memory.put("used", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        return memory;
    }

    private boolean isModelLoaded() {
        try {
            // Use dynamic path from config
            return new File(config.getModelFilePath()).exists();
        } catch (Exception e) {
            return false;
        }
    }
}
