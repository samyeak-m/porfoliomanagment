package com.stockmanagment.porfoliomanagment.controller;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockmanagment.porfoliomanagment.config.LstmConfig;
import com.stockmanagment.porfoliomanagment.dto.PredictionRequestDTO;
import com.stockmanagment.porfoliomanagment.dto.PredictionResponseDTO;
import com.stockmanagment.porfoliomanagment.service.LstmService;
import com.stockmanagment.porfoliomanagment.service.nepse.VarCalculationService;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.database.DatabaseHelper;

@RestController
@RequestMapping("/api/lstm")
public class PredictionController {

    @Autowired
    private LstmService lstmService;

    @Autowired
    private LstmConfig config;

    @Autowired
    private VarCalculationService varCalculationService;

    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> predict(@RequestBody PredictionRequestDTO request) {
        try {
            // Get LSTM prediction
            PredictionResponseDTO lstmPrediction = lstmService.predict(request.getStockSymbol());

            // Get VaR parameters from request or use defaults
            int daysOfInvestment = request.getDaysOfInvestment() != null ? request.getDaysOfInvestment() : 25;
            String confidenceLevelStr = request.getConfidenceLevel();
            double confidenceLevel;

            if ("dynamic".equals(confidenceLevelStr)) {
                confidenceLevel = varCalculationService.calculateDynamicConfidenceLevel(request.getStockSymbol());
            } else {
                confidenceLevel = Double.parseDouble(confidenceLevelStr != null ? confidenceLevelStr : "0.95");
            }

            // Calculate VaR
            double varValue = varCalculationService.calculateVaR(request.getStockSymbol(), daysOfInvestment,
                    confidenceLevel);
            double initialPrice = varCalculationService.getInitialStockPrice(request.getStockSymbol());
            double varPercentage = (varValue / initialPrice) * 100;

            // Combine results
            Map<String, Object> response = new HashMap<>();
            response.put("stockSymbol", lstmPrediction.getStockSymbol());
            response.put("prediction", lstmPrediction.getPrediction());
            response.put("lastClose", lstmPrediction.getLastClose());
            response.put("pointChange", lstmPrediction.getPointChange());
            response.put("priceChange", lstmPrediction.getPriceChange());
            response.put("predictionDate", lstmPrediction.getPredictionDate());

            // Add VaR data
            response.put("varValue", varValue);
            response.put("varPercentage", varPercentage);
            response.put("confidenceLevel", confidenceLevel * 100);
            response.put("initialPrice", initialPrice);
            response.put("daysOfInvestment", daysOfInvestment);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Prediction failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
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
    public ResponseEntity<?> getMetrics() {
        try {
            String path = config.getOutputDir() + "/confusion.txt";
            File metricsFile = new File(path);

            if (!metricsFile.exists()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "not_trained");
                response.put("message", "Model not trained yet");
                response.put("instruction", "Please train the model first to view metrics");
                response.put("expectedPath", path);

                return ResponseEntity.ok(response);
            }

            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain")
                    .body(content);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error reading model metrics");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("instruction", "Please train the model first");

            return ResponseEntity.status(500).body(errorResponse);
        }
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

    @GetMapping("/stock-symbols")
    public List<String> getStockSymbols() {
        try {
            DatabaseHelper dbHelper = new DatabaseHelper();
            List<String> symbols = dbHelper.getAllStockTableNames();

            return symbols.stream()
                    .map(String::toUpperCase)
                    .sorted()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error fetching stock symbols: " + e.getMessage());
            return new ArrayList<>();
        }
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
            return new File(config.getModelFilePath()).exists();
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping("/model-status")
    public Map<String, Object> getModelStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            String modelPath = config.getModelFilePath();
            boolean modelExists = new File(modelPath).exists();

            String metricsPath = config.getOutputDir() + "/confusion.txt";
            boolean metricsExists = new File(metricsPath).exists();

            status.put("modelTrained", modelExists && metricsExists);
            status.put("modelFileExists", modelExists);
            status.put("metricsFileExists", metricsExists);
            status.put("modelPath", modelPath);
            status.put("metricsPath", metricsPath);

            if (modelExists && metricsExists) {
                status.put("status", "ready");
                status.put("message", "Model is trained and ready for predictions");
            } else if (modelExists && !metricsExists) {
                status.put("status", "partial");
                status.put("message", "Model exists but metrics are missing");
            } else {
                status.put("status", "not_trained");
                status.put("message", "Model not trained yet");
            }

        } catch (Exception e) {
            status.put("status", "error");
            status.put("message", "Error checking model status: " + e.getMessage());
        }

        return status;
    }
}
