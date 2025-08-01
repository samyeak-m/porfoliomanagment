package com.stockmanagment.porfoliomanagment.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.stockmanagment.porfoliomanagment.config.LstmConfig;
import com.stockmanagment.porfoliomanagment.dto.PredictionResponseDTO;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.database.DatabaseHelper;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.lstm.LSTMNetwork;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.CustomChartUtils;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.DataPreprocessor;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.TechnicalIndicators;

import jakarta.annotation.PostConstruct;

@Service
public class LstmService {
    private static final Logger LOGGER = Logger.getLogger(LstmService.class.getName());
    
    private final LstmConfig config;
    
    private double[] min;
    private double[] max;
    
    private LSTMNetwork lstm;

    private final List<Integer> epochList = new ArrayList<>();
    private final List<Double> accuracyList = new ArrayList<>();
    private final List<Double> lossList = new ArrayList<>();
    private final List<Double> validationAccuracyList = new ArrayList<>();
    private final List<Double> validationLossList = new ArrayList<>();

    private volatile String currentTrainingMessage = "";
    private volatile int currentEpoch = 0;
    private volatile double currentProgress = 0.0;
    private volatile boolean isTraining = false;

    public LstmService(LstmConfig config) {
        this.config = config;
    }
    
    @PostConstruct
    public void init() {
        lstm = LSTMNetwork.loadModel(config.getModelFilePath());
    }

    public void train() {
        trainingStartTime = System.currentTimeMillis();
        
        try {
            isTraining = true;
            currentTrainingMessage = "Initializing neural network...";
            currentProgress = 0.0;
            
            // ADD: Check memory at start of training
            checkMemoryAndCleanup();
            
            createDirectory(config.getOutputDir());
            currentTrainingMessage = "Creating output directory completed";
            currentProgress = 5.0;
            
            DatabaseHelper dbHelper = new DatabaseHelper();
            currentTrainingMessage = "Connecting to database...";
            currentProgress = 10.0;
            
            // ADD: Check memory before loading data
            checkMemoryAndCleanup();
            
            if (lstm == null) {
                currentTrainingMessage = "Loading stock data from database...";
                currentProgress = 15.0;
                
                List<String> tableNames = dbHelper.getAllStockTableNames();
                currentTrainingMessage = "Found " + tableNames.size() + " stock tables";
                currentProgress = 20.0;
                
                List<double[]> allStockData = new ArrayList<>();
                for (String tableName : tableNames) {
                    allStockData.addAll(dbHelper.loadStockData(tableName));
                    currentTrainingMessage = "Loading data from " + tableName + "...";
                }
                currentProgress = 30.0;
                
                double[][] stockDataArray = allStockData.toArray(new double[0][]);
                currentTrainingMessage = "Loaded " + stockDataArray.length + " data points";
                currentProgress = 35.0;
                
                currentTrainingMessage = "Calculating technical indicators (EMA, SMA, RSI, MACD, Bollinger Bands)...";
                double[][] technicalIndicators = TechnicalIndicators.calculate(stockDataArray, 16, 3);
                currentProgress = 45.0;
                
                currentTrainingMessage = "Adding technical features to dataset...";
                double[][] extendedData = DataPreprocessor.addFeatures(stockDataArray, technicalIndicators);
                currentProgress = 50.0;
                
                currentTrainingMessage = "Preprocessing and normalizing data...";
                double[][][] preprocessedData = preprocessData(extendedData, 0.6);
                currentProgress = 55.0;
                
                double[][] trainData = preprocessedData[0];
                double[][] testData = preprocessedData[1];
                
                currentTrainingMessage = "Splitting data - Train: " + trainData.length + ", Test: " + testData.length;
                currentProgress = 60.0;
                
                currentTrainingMessage = "Creating new LSTM network...";
                lstm = new LSTMNetwork(config.getInputSize(), config.getHiddenSize(), 
                                     config.getOutputSize(), config.getDenseSize(), min, max);
                
                currentTrainingMessage = "Starting LSTM training with " + config.getEpochs() + " epochs...";
                
                // ADD: Check memory during training
                checkMemoryAndCleanup();
                
                double[] averages = trainModel(lstm, trainData, testData, config.getEpochs(), config.getTrainingRate(), min, max);
                
                currentTrainingMessage = "Training completed. Saving model...";
                currentProgress = 95.0;
                
                lstm.saveModel(config.getModelFilePath());
                
                int[][] confusionMatrix = lstm.computeConfusionMatrix(testData, testData[testData.length-1][1], config.getThreshold());
                double[][] classMetrics = printConfusionMatrix(confusionMatrix);
                
                logFile(averages[0], averages[1], averages[0], averages[1], confusionMatrix,
                       classMetrics[1][0], classMetrics[1][1], classMetrics[1][2],
                       classMetrics[0][0], classMetrics[0][1], classMetrics[0][2],
                       trainData, testData, testData);
                
                generateTrainingCharts();
                saveLastTrainingDate();
                
                optimizeMemoryUsage();
                
                System.out.println("Training completed successfully!");
                
            } else {
                currentTrainingMessage = "Model already exists, loading saved model...";
                currentProgress = 100.0;
                System.out.println("Model already trained. Use incremental learning for updates.");
            }
            
            isTraining = false;
            currentTrainingMessage = "Training completed successfully!";
            currentProgress = 100.0;
            
        } catch (Exception e) {
            isTraining = false;
            currentTrainingMessage = "Training failed: " + e.getMessage();
            System.err.println("Training failed: " + e.getMessage());
            e.printStackTrace();
            optimizeMemoryUsage();
            throw new RuntimeException("Training failed: " + e.getMessage(), e);
        }
    }

    private boolean shouldPerformIncrementalLearning() {
        try {
            File dateFile = new File(config.getLastTrainingDateFile());
            if (!dateFile.exists()) {
                return true;
            }
            
            try (BufferedReader reader = new BufferedReader(new FileReader(dateFile))) {
                String lastDateStr = reader.readLine();
                LocalDate lastTrainingDate = LocalDate.parse(lastDateStr);
                LocalDate today = LocalDate.now();
                
                long daysSinceLastTraining = ChronoUnit.DAYS.between(lastTrainingDate, today);
                System.out.println("Days since last training: " + daysSinceLastTraining);
                
                return daysSinceLastTraining >= config.getRetrainThresholdDays();
            }
        } catch (Exception e) {
            System.err.println("Error checking last training date: " + e.getMessage());
            return true;
        }
    }

    private void saveLastTrainingDate() {
        try {
            createDirectory(config.getOutputDir());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(config.getLastTrainingDateFile()))) {
                writer.write(LocalDate.now().toString());
            }
        } catch (IOException e) {
            System.err.println("Error saving training date: " + e.getMessage());
        }
    }

    public void incrementalLearning() {
        try {
            // ADD: Check memory at start
            checkMemoryAndCleanup();
            
            if (!shouldPerformIncrementalLearning()) {
                System.out.println("No incremental learning needed yet.");
                return;
            }
            
            DatabaseHelper dbHelper = new DatabaseHelper();
            LSTMNetwork lstm = LSTMNetwork.loadModel(config.getModelFilePath());
            
            if (lstm == null) {
                System.out.println("No existing model found. Performing full training.");
                train();
                return;
            }
            
            System.out.println("Starting incremental learning...");
            
            List<String> tableNames = dbHelper.getAllStockTableNames();
            List<double[]> newStockData = getNewDataSinceLastTraining(dbHelper, tableNames);
            
            if (newStockData.isEmpty()) {
                System.out.println("No new data available for incremental learning.");
                return;
            }
            
            System.out.println("Found " + newStockData.size() + " new data points for incremental learning");
            
            double[][] newStockDataArray = newStockData.toArray(new double[0][]);
            double[][] technicalIndicators = TechnicalIndicators.calculate(newStockDataArray, 16, 3);
            double[][] extendedData = DataPreprocessor.addFeatures(newStockDataArray, technicalIndicators);
            
            min = lstm.getMin();
            max = lstm.getMax();
            
            double[][] normalizedNewData = DataPreprocessor.normalize(extendedData, min, max);
            
            // ADD: Check memory after data loading
            checkMemoryAndCleanup();
            
            performIncrementalTraining(lstm, normalizedNewData);
            
            lstm.saveModel(config.getModelFilePath());
            saveLastTrainingDate();
            
            // ADD: Final cleanup
            optimizeMemoryUsage();
            
            System.out.println("Incremental learning completed successfully!");
            
        } catch (Exception e) {
            optimizeMemoryUsage();
            throw new RuntimeException("Incremental learning failed: " + e.getMessage(), e);
        }
    }

    private List<double[]> getNewDataSinceLastTraining(DatabaseHelper dbHelper, List<String> tableNames) throws SQLException {
        List<double[]> newData = new ArrayList<>();
        
        try {
            File dateFile = new File(config.getLastTrainingDateFile());
            LocalDate lastTrainingDate = LocalDate.now().minusDays(config.getRetrainThresholdDays());
            
            if (dateFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(dateFile))) {
                    String lastDateStr = reader.readLine();
                    lastTrainingDate = LocalDate.parse(lastDateStr);
                }
            }
            
            for (String tableName : tableNames) {
                List<double[]> stockData = dbHelper.loadStockDataAfterDate(tableName, lastTrainingDate);
                newData.addAll(stockData);
            }
            
        } catch (Exception e) {
            System.err.println("Error getting new data: " + e.getMessage());
        }
        
        return newData;
    }

    private void performIncrementalTraining(LSTMNetwork lstm, double[][] newData) {
        System.out.println("Performing incremental training on " + newData.length + " new samples");
        
        double incrementalLearningRate = config.getTrainingRate() * 0.1;
        int incrementalEpochs = 5;
        
        for (int epoch = 0; epoch < incrementalEpochs; epoch++) {
            // ADD: Check memory at start of each epoch
            checkMemoryAndCleanup();
            
            System.out.println("Incremental epoch " + (epoch + 1) + "/" + incrementalEpochs);
            
            List<double[]> dataList = Arrays.asList(newData);
            Collections.shuffle(dataList);
            double[][] shuffledData = dataList.toArray(new double[0][]);
            
            for (double[] data : shuffledData) {
                double[] hiddenState = new double[lstm.getHiddenSize()];
                double[] cellState = new double[lstm.getHiddenSize()];
                
                double[] input = Arrays.copyOf(data, config.getInputSize());
                double[] target = new double[]{data[data.length - 1]};
                
                double[] output = lstm.forward(input, hiddenState, cellState);
                if (output == null) {
                    continue;
                }
                
                lstm.backpropagate(input, target, incrementalLearningRate);
            }
            
            double accuracy = calculateIncrementalAccuracy(lstm, shuffledData);
            System.out.println("Incremental Epoch " + epoch + " Accuracy: " + String.format("%.4f", accuracy));
            
            // ADD: Check memory after each epoch
            if (isMemoryLow()) {
                optimizeMemoryUsage();
            }
        }
    }

    private double calculateIncrementalAccuracy(LSTMNetwork lstm, double[][] data) {
        double totalAccuracy = 0;
        int validSamples = 0;
        
        for (int i = 0; i < data.length - 1; i++) {
            double[] input = Arrays.copyOf(data[i], config.getInputSize());
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            
            if (output == null) continue;
            
            double prediction = output[0];
            double actual = data[i + 1][1];
            double currentClosePrice = data[i][1];
            
            prediction = applyPredictionConstraints(prediction, currentClosePrice);
            double accuracy = calculatePredictionAccuracy(prediction, actual, currentClosePrice);
            
            totalAccuracy += accuracy;
            validSamples++;
        }
        
        return validSamples > 0 ? totalAccuracy / validSamples : 0;
    }

    private double[][] printConfusionMatrix(int[][] matrix) {
        int tp = matrix[0][0];
        int fn = matrix[1][0];
        int fp = matrix[0][1];
        int tn = matrix[1][1];

        double precisionPositive = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0;
        double recallPositive = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0;
        double f1ScorePositive = (precisionPositive + recallPositive) > 0 ? 2 * (precisionPositive * recallPositive) / (precisionPositive + recallPositive) : 0;

        double precisionNegative = (tn + fn) > 0 ? (double) tn / (tn + fn) : 0;
        double recallNegative = (tn + fp) > 0 ? (double) tn / (tn + fp) : 0;
        double f1ScoreNegative = (precisionNegative + recallNegative) > 0 ? 2 * (precisionNegative * recallNegative) / (precisionNegative + recallNegative) : 0;

        System.out.println("Confusion Matrix:");
        System.out.println("TP: " + tp + ", FN: " + fn);
        System.out.println("FP: " + fp + ", TN: " + tn);

        System.out.println("Positive Class:");
        System.out.println("Precision: " + String.format("%.4f", precisionPositive));
        System.out.println("Recall: " + String.format("%.4f", recallPositive));
        System.out.println("F1 Score: " + String.format("%.4f", f1ScorePositive));

        System.out.println("Negative Class:");
        System.out.println("Precision: " + String.format("%.4f", precisionNegative));
        System.out.println("Recall: " + String.format("%.4f", recallNegative));
        System.out.println("F1 Score: " + String.format("%.4f", f1ScoreNegative));

        return new double[][]{
                {precisionPositive, recallPositive, f1ScorePositive},
                {precisionNegative, recallNegative, f1ScoreNegative}
        };
    }

    private double[] trainModel(LSTMNetwork lstm, double[][] trainData, double[][] validationData, int epochs, double learningRate, double[] min, double[] max) {
        double totalAccuracy = 0;
        double totalLoss = 0;

        for (int epoch = 0; epoch < epochs; epoch++) {
            currentEpoch = epoch + 1;
            currentTrainingMessage = "Training epoch " + (epoch + 1) + "/" + epochs + " - Learning patterns...";
            
            currentProgress = 60.0 + (35.0 * (epoch + 1) / epochs);
            
            long startTime = System.currentTimeMillis();
            
            // ADD: Check memory every 10 epochs
            if (epoch % 10 == 0) {
                checkMemoryAndCleanup();
            }
            
            int totalDataPoints = trainData.length;
            int batchSize = config.getBatchSize();
            int batches = totalDataPoints / batchSize;

            for (int batch = 0; batch < batches; batch++) {
                double[][] batchData = Arrays.copyOfRange(trainData, batch * batchSize, (batch + 1) * batchSize);
                for (double[] data : batchData) {
                    double[] hiddenState = new double[lstm.getHiddenSize()];
                    double[] cellState = new double[lstm.getHiddenSize()];
                    
                    double[] input = Arrays.copyOf(data, config.getInputSize());
                    double[] target = new double[]{data[data.length - 1]};
                    
                    double[] output = lstm.forward(input, hiddenState, cellState);
                    if (output == null) {
                        LOGGER.severe("NaN value encountered during forward pass. Stopping training.");
                        return new double[]{0, 0};
                    }
                    
                    lstm.backpropagate(input, target, learningRate);
                }
            }

            double accuracy = testModel(lstm, trainData);
            double epochLoss = calculateLoss(lstm, trainData);

            double validationAccuracy = testModel(lstm, validationData);
            double validationLoss = calculateValidationLoss(lstm, validationData);

            long endTime = System.currentTimeMillis();
            long elapsedTimeMillis = endTime - startTime;

            epochList.add(epoch);
            accuracyList.add(accuracy);
            lossList.add(epochLoss);
            validationAccuracyList.add(validationAccuracy);
            validationLossList.add(validationLoss);

            logTrainingProgress(epoch, accuracy, epochLoss, validationAccuracy, validationLoss, elapsedTimeMillis);

            totalAccuracy += accuracy;
            totalLoss += epochLoss;
            
            currentTrainingMessage = String.format(
                "Epoch %d/%d - Acc: %.3f, Loss: %.3f, Val_Acc: %.3f, Val_Loss: %.3f", 
                epoch + 1, epochs, accuracy, epochLoss, validationAccuracy, validationLoss
            );

            // ADD: Check memory after heavy operations
            if (epoch % 25 == 0) {
                checkMemoryAndCleanup();
            }

            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        
        currentTrainingMessage = "Calculating final metrics and saving model...";
        currentProgress = 95.0;
        
        double averageAccuracy = totalAccuracy / epochs;
        double averageLoss = totalLoss / epochs;

        LOGGER.log(Level.INFO, String.format("Overall Average Accuracy: %.2f", averageAccuracy));
        LOGGER.log(Level.INFO, String.format("Overall Average Loss: %.2f", averageLoss));

        return new double[]{averageAccuracy, averageLoss};
    }

    private void logTrainingProgress(int epoch, double accuracy, double loss, 
                               double valAccuracy, double valLoss, long elapsedTime) {
    
    String progress = String.format(
        "Epoch %d/%d (%.1f%%) - Acc: %.4f, Loss: %.4f, Val_Acc: %.4f, Val_Loss: %.4f, Time: %dms",
        epoch + 1, config.getEpochs(), ((epoch + 1) * 100.0 / config.getEpochs()), 
        accuracy, loss, valAccuracy, valLoss, elapsedTime
    );
    
    LOGGER.log(Level.INFO, progress);
    
    try (FileWriter fw = new FileWriter(config.getOutputDir() + "/training_log.txt", true)) {
        fw.write(progress + "\n");
    } catch (IOException e) {
        System.err.println("Failed to write training log: " + e.getMessage());
    }
}

    private double calculateValidationLoss(LSTMNetwork lstm, double[][] validationData) {
        return calculateLoss(lstm, validationData);
    }

    private double testModel(LSTMNetwork lstm, double[][] testData) {
        double totalAccuracy = 0;

        for (int i = 0; i < testData.length - 1; i++) {
            double[] input = Arrays.copyOf(testData[i], config.getInputSize());
            checkForNaN1D(input, "input to LSTM (testModel)");
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            if (output == null) {
                continue;
            }
            double prediction = output[0];
            double actual = testData[i + 1][1];
            double currentClosePrice = testData[i][1];

            prediction = applyPredictionConstraints(prediction, currentClosePrice);

            double accuracy = calculatePredictionAccuracy(prediction, actual, currentClosePrice);
            totalAccuracy += accuracy;

            if (i < 5) {
                System.out.printf("Raw prediction: %.4f, Last close: %.4f, After constraints: %.4f, Actual: %.4f%n", 
                    output[0], currentClosePrice, prediction, actual);
            }
        }
        return totalAccuracy / (testData.length - 1);
    }

    private double applyPredictionConstraints(double prediction, double lastClosePrice) {
        double maxDailyChange = 0.05;
        double minPrice = lastClosePrice * (1 - maxDailyChange);
        double maxPrice = lastClosePrice * (1 + maxDailyChange);

        if (prediction < minPrice) {
            prediction = minPrice;
        } else if (prediction > maxPrice) {
            prediction = maxPrice;
        }

        return prediction;
    }

    private double calculatePredictionAccuracy(double prediction, double actual, double currentClosePrice) {
        double maxChange = 0.05 * currentClosePrice;
        double diff = Math.abs(prediction - actual);

        if (diff > maxChange) {
            return 0;
        }

        double accuracy = 1 - (diff / maxChange);
        return accuracy;
    }

    private double calculateLoss(LSTMNetwork lstm, double[][] data) {
        double totalLoss = 0;
        double maxChange = 0.05;

        for (int i = 0; i < data.length - 1; i++) {
            double[] input = Arrays.copyOf(data[i], config.getInputSize());
            checkForNaN1D(input, "input to calculateLoss");
            
            double lastClosePrice = data[i][1];
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            
            if (output == null) {
                continue;
            }
            
            double prediction = output[0];

            prediction = applyPredictionConstraints(prediction, lastClosePrice);

            double actual = data[i + 1][1];
            double diff = Math.abs(prediction - actual);
            double tolerance = maxChange * actual;

            double loss;
            if (diff > tolerance) {
                loss = 1.0;
            } else {
                loss = diff / tolerance;
            }

            totalLoss += loss;
        }

        return totalLoss / (data.length - 1);
    }

    public void logFile(double finalTestAccuracy, double finalTestLoss, double averageAccuracy, double averageLoss, int[][] confusionMatrix,
                               double precisionNegative, double recallNegative, double f1ScoreNegative, double precisionPositive, double recallPositive, double f1ScorePositive, double[][] trainData, double[][] validationData, double[][] finalTestData) {
        String logFileName = config.getOutputDir() + File.separator + "confusion.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName))) {
        
            writer.write("=== MODEL CONFIGURATION ===\n");
            writer.write("Version: " + config.getVersion() + "\n");
            writer.write("Hidden Size: " + config.getHiddenSize() + "\n");
            writer.write("Dense Size: " + config.getDenseSize() + "\n");
            writer.write("Input Size: " + config.getInputSize() + "\n");
            writer.write("Output Size: " + config.getOutputSize() + "\n");
            writer.write("Epochs: " + config.getEpochs() + "\n");
            writer.write("Batch Size: " + config.getBatchSize() + "\n");
            writer.write("Training Rate: " + config.getTrainingRate() + "\n");
            writer.write("Threshold: " + config.getThreshold() + "\n");
            writer.write("Interval: " + config.getInterval() + "\n");
            writer.write("\n");
            
            writer.write("=== DATASET INFORMATION ===\n");
            writer.write("Training data size: " + trainData.length + "\n");
            writer.write("Validation data size: " + validationData.length + "\n");
            writer.write("Final test data size: " + finalTestData.length + "\n");
            writer.write("Total data points: " + (trainData.length + validationData.length + finalTestData.length) + "\n");
            writer.write("Train/Validation/Test split: " + 
                String.format("%.1f%%/%.1f%%/%.1f%%", 
                    (trainData.length / (double)(trainData.length + validationData.length + finalTestData.length)) * 100,
                    (validationData.length / (double)(trainData.length + validationData.length + finalTestData.length)) * 100,
                    (finalTestData.length / (double)(trainData.length + validationData.length + finalTestData.length)) * 100) + "\n");
            writer.write("\n");

            writer.write("=== MODEL PERFORMANCE ===\n");
            writer.write("Final Test Accuracy: " + String.format("%.4f", finalTestAccuracy) + "\n");
            writer.write("Final Test Loss: " + String.format("%.4f", finalTestLoss) + "\n");
            writer.write("Overall Average Training Accuracy: " + String.format("%.4f", averageAccuracy) + "\n");
            writer.write("Overall Average Training Loss: " + String.format("%.4f", averageLoss) + "\n");
            writer.write("\n");

            int tp = confusionMatrix[0][0];
            int fn = confusionMatrix[1][0];
            int fp = confusionMatrix[0][1];
            int tn = confusionMatrix[1][1];
            int total = tp + fn + fp + tn;

            writer.write("=== CONFUSION MATRIX ===\n");
            writer.write("True Positives (TP): " + tp + "\n");
            writer.write("False Negatives (FN): " + fn + "\n");
            writer.write("False Positives (FP): " + fp + "\n");
            writer.write("True Negatives (TN): " + tn + "\n");
            writer.write("Total Predictions: " + total + "\n");
            writer.write("\n");
            
            writer.write("Matrix Format:\n");
            writer.write("             Predicted\n");
            writer.write("           Pos    Neg\n");
            writer.write("Actual Pos " + String.format("%3d", tp) + "    " + String.format("%3d", fn) + "\n");
            writer.write("       Neg " + String.format("%3d", fp) + "    " + String.format("%3d", tn) + "\n");
            writer.write("\n");

            writer.write("=== POSITIVE CLASS METRICS ===\n");
            writer.write("Precision: " + String.format("%.4f", precisionPositive) + " (" + tp + "/" + (tp + fp) + ")\n");
            writer.write("Recall (Sensitivity): " + String.format("%.4f", recallPositive) + " (" + tp + "/" + (tp + fn) + ")\n");
            writer.write("F1 Score: " + String.format("%.4f", f1ScorePositive) + "\n");
            writer.write("\n");

            writer.write("=== NEGATIVE CLASS METRICS ===\n");
            writer.write("Precision: " + String.format("%.4f", precisionNegative) + " (" + tn + "/" + (tn + fn) + ")\n");
            writer.write("Recall (Specificity): " + String.format("%.4f", recallNegative) + " (" + tn + "/" + (tn + fp) + ")\n");
            writer.write("F1 Score: " + String.format("%.4f", f1ScoreNegative) + "\n");
            writer.write("\n");

            double accuracy = (double)(tp + tn) / total;
            double errorRate = (double)(fp + fn) / total;
            
            writer.write("=== OVERALL METRICS ===\n");
            writer.write("Overall Accuracy: " + String.format("%.4f", accuracy) + " (" + (tp + tn) + "/" + total + ")\n");
            writer.write("Overall Error Rate: " + String.format("%.4f", errorRate) + " (" + (fp + fn) + "/" + total + ")\n");
            writer.write("\n");

            writer.write("=== TECHNICAL INDICATORS USED ===\n");
            writer.write("1. EMA (Exponential Moving Average) - Period: 16\n");
            writer.write("2. SMA (Simple Moving Average) - Period: 20\n");
            writer.write("3. RSI (Relative Strength Index) - Period: 3\n");
            writer.write("4. ATR (Average True Range) - Period: 14\n");
            writer.write("5. MACD (Moving Average Convergence Divergence) - Periods: 12,26,9\n");
            writer.write("6. Bollinger Bands - Period: 20, Std Dev: 2.0\n");
            writer.write("7. Stochastic Oscillator - Period: 14\n");
            writer.write("\n");

            writer.write("=== TECHNICAL INDICATORS CALCULATION DETAILS ===\n");
            writer.write("Calculation Method: Dynamic from actual training data\n");
            writer.write("Data Source: First " + Math.min(5, trainData.length) + " rows of training dataset\n");
            writer.write("EMA Period: 16, SMA Period: 20, RSI Period: 3\n");
            writer.write("MACD Periods: 12,26,9 | BB Period: 20, StdDev: 2.0 | Stochastic Period: 14\n");
            writer.write("\n");

            writer.write("=== SAMPLE TECHNICAL INDICATORS (First 5 Rows) ===\n");

            if (trainData.length > 0) {
                try {
                    int sampleRows = Math.min(5, trainData.length);
                    
                    double[][] sampleData = new double[sampleRows][6];
                    for (int i = 0; i < sampleRows; i++) {
                        sampleData[i][0] = trainData[i][0];
                        sampleData[i][1] = denormalizeValue(trainData[i][1], min[1], max[1]);
                        sampleData[i][2] = denormalizeValue(trainData[i][2], min[2], max[2]);
                        sampleData[i][3] = denormalizeValue(trainData[i][3], min[3], max[3]);
                        sampleData[i][4] = denormalizeValue(trainData[i][4], min[4], max[4]);
                        sampleData[i][5] = denormalizeValue(trainData[i][5], min[5], max[5]);
                    }
                    
                    double[][] indicators = TechnicalIndicators.calculate(sampleData, 16, 3);
                    
                    for (int i = 0; i < sampleRows; i++) {
                        writer.write(String.format(
                            "Row %d: Close=%.2f, EMA=%.2f, SMA=%.2f, RSI=%.2f, ATR=%.2f, MACD=%.2f, Signal=%.2f, Histogram=%.2f, BB_Upper=%.2f, BB_Lower=%.2f, Stoch_K=%.2f, Stoch_D=%.2f%n",
                            i,
                            sampleData[i][1],
                            indicators[i][0],
                            indicators[i][1],  
                            indicators[i][2],
                            indicators[i][3],
                            indicators[i][4],
                            indicators[i][5],
                            indicators[i][6],
                            indicators[i][8],
                            indicators[i][9],
                            indicators[i][10],
                            indicators[i][11]
                        ));
                    }
                    
                    writer.write("\nNote: Values calculated dynamically from actual training data\n");
                    writer.write("Close prices denormalized from range [" + String.format("%.2f", min[1]) + ", " + String.format("%.2f", max[1]) + "]\n");
                    
                } catch (Exception e) {
                    writer.write("Error calculating dynamic technical indicators: " + e.getMessage() + "\n");
                    e.printStackTrace();
                }
            } else {
                writer.write("No training data available for technical indicators calculation.\n");
            }
            writer.write("\n");

            writer.write("=== TRAINING EPOCH DETAILS ===\n");
            for (int i = 0; i < epochList.size(); i++) {
                writer.write(String.format("Epoch %3d: Accuracy = %.4f, Loss = %.4f, Val_Accuracy = %.4f, Val_Loss = %.4f\n",
                        epochList.get(i), accuracyList.get(i), lossList.get(i), validationAccuracyList.get(i), validationLossList.get(i)));
            }

        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    private void createDirectory(String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private double[][][] preprocessData(double[][] data, double trainSplitRatio) {
        int trainSize = (int) (data.length * trainSplitRatio);
        double[][] trainData = Arrays.copyOfRange(data, 0, trainSize);
        double[][] testData = Arrays.copyOfRange(data, trainSize, data.length);
        
        System.out.println("Using original unbalanced data for testing");
        
        min = DataPreprocessor.calculateMin(data);
        max = DataPreprocessor.calculateMax(data);

        double bufferPercentage = 0.40;

        for (int i = 1; i < min.length; i++) {
            double actualMin = min[i];
            double actualMax = max[i];

            double bufferedMin = actualMin - (bufferPercentage * (actualMax - actualMin));
            double bufferedMax = actualMax + (bufferPercentage * (actualMax - actualMin));

            if (bufferedMin < 0) {
                bufferedMin = 0;
            }

            min[i] = bufferedMin;
            max[i] = bufferedMax;
        }

        System.out.println("Min close price: " + min[1] + ", Max close price: " + max[1]);
        System.out.println("Price range: " + (max[1] - min[1]));

        trainData = DataPreprocessor.normalize(trainData, min, max);
        testData = DataPreprocessor.normalize(testData, min, max);
        
        return new double[][][]{trainData, testData};
    }

    private void checkForNaN1D(double[] data, String label) {
        boolean found = false;
        for (int i = 0; i < data.length; i++) {
            if (Double.isNaN(data[i]) || Double.isInfinite(data[i])) {
                System.err.println("Invalid value in " + label + " at [" + i + "]: " + data[i]);
                found = true;
            }
        }
        if (found) {
            System.err.println("Full input vector for " + label + ": " + Arrays.toString(data));
        }
    }
    private double denormalizeValue(double normalizedValue, double minValue, double maxValue) {
        return normalizedValue * (maxValue - minValue) + minValue;
    }

    public PredictionResponseDTO predict(String stockSymbol) {
        try {
            // ADD: Check memory before prediction
            checkMemoryAndCleanup();
            
            if (lstm == null) {
                throw new RuntimeException("No trained model found. Please train the model first.");
            }
            
            min = lstm.getMin();
            max = lstm.getMax();
            
            if (min == null || max == null) {
                throw new RuntimeException("Model normalization parameters not found. Please retrain the model.");
            }
            
            String normalizedStockSymbol = stockSymbol.toLowerCase();
            
            DatabaseHelper dbHelper = new DatabaseHelper();
            List<double[]> stockData = dbHelper.loadStockData(normalizedStockSymbol);
            
            if (stockData.isEmpty()) {
                throw new RuntimeException("No data found for stock symbol: " + stockSymbol);
            }
            
            double[][] stockDataArray = stockData.toArray(new double[0][]);
            double[][] technicalIndicators = TechnicalIndicators.calculate(stockDataArray, 16, 3);
            double[][] extendedData = DataPreprocessor.addFeatures(stockDataArray, technicalIndicators);
            
            double originalLastClose = extendedData[extendedData.length - 1][1];
            
            extendedData = DataPreprocessor.normalize(extendedData, min, max);

            double[] input = Arrays.copyOf(extendedData[extendedData.length - 1], config.getInputSize());
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            
            if (output == null) {
                throw new RuntimeException("LSTM forward pass returned null");
            }
            
            double prediction = output[0];
            
            double denormalizedPrediction = prediction * (max[1] - min[1]) + min[1];
            
            denormalizedPrediction = applyPredictionConstraints(denormalizedPrediction, originalLastClose);

            double pointChange = denormalizedPrediction - originalLastClose;
            double priceChange = (pointChange / originalLastClose) * 100;

            PredictionResponseDTO response = new PredictionResponseDTO();
            response.setStockSymbol(stockSymbol.toUpperCase());
            response.setPrediction(denormalizedPrediction);
            response.setLastClose(originalLastClose);
            response.setPointChange(pointChange);
            response.setPriceChange(priceChange);
            response.setPredictionDate(LocalDate.now().toString());

            return response;
        } catch (Exception e) {
            // ADD: Cleanup on error
            if (isMemoryLow()) {
                optimizeMemoryUsage();
            }
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }

    // ADD: Helper method to check if memory is low
    private boolean isMemoryLow() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        return memoryUsagePercent > 75;
    }

    // ADD: Enhanced memory monitoring with logging
    private void checkMemoryAndCleanup() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;  // MB
        long totalMemory = runtime.totalMemory() / 1024 / 1024;  // MB
        long freeMemory = runtime.freeMemory() / 1024 / 1024;  // MB
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        LOGGER.log(Level.INFO, String.format(
            "Memory Status - Used: %d MB, Free: %d MB, Total: %d MB, Max: %d MB (%.1f%% used)",
            usedMemory, freeMemory, totalMemory, maxMemory, memoryUsagePercent
        ));
        
        if (memoryUsagePercent > 80) {
            LOGGER.log(Level.WARNING, "High memory usage detected: " + String.format("%.1f%%", memoryUsagePercent));
            optimizeMemoryUsage();
            
            System.gc();
            
            runtime = Runtime.getRuntime();
            long newUsedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            LOGGER.log(Level.INFO, String.format("Memory after cleanup: %d MB", newUsedMemory));
        }
    }

    @Scheduled(fixedRate = 30000)
    public void periodicMemoryCheck() {
        if (isTraining) {
            checkMemoryAndCleanup();
        }
    }

    public Map<String, Object> getTrainingStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            File dateFile = new File(config.getLastTrainingDateFile());
            LocalDate lastTrainingDate = null;
            long daysSinceTraining = 0;
            boolean needsIncremental = true;
            
            if (dateFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(dateFile))) {
                    String lastDateStr = reader.readLine();
                    if (lastDateStr != null && !lastDateStr.trim().isEmpty()) {
                        lastTrainingDate = LocalDate.parse(lastDateStr);
                        LocalDate today = LocalDate.now();
                        daysSinceTraining = ChronoUnit.DAYS.between(lastTrainingDate, today);
                        needsIncremental = daysSinceTraining >= config.getRetrainThresholdDays();
                    }
                } catch (Exception e) {
                    System.err.println("Error reading training date: " + e.getMessage());
                }
            }
            
            File modelFile = new File(config.getModelFilePath());
            boolean modelExists = modelFile.exists();
            
            status.put("lastTrainingDate", lastTrainingDate != null ? lastTrainingDate.toString() : "Never");
            status.put("daysSinceTraining", daysSinceTraining);
            status.put("needsIncremental", needsIncremental);
            status.put("modelExists", modelExists);
            status.put("retrainThresholdDays", config.getRetrainThresholdDays());
            status.put("currentDate", LocalDate.now().toString());
            
            if (modelExists) {
                status.put("modelPath", config.getModelFilePath());
                status.put("modelSize", String.format("%.2f MB", modelFile.length() / (1024.0 * 1024.0)));
            }
            
            status.put("version", config.getVersion());
            status.put("epochs", config.getEpochs());
            status.put("batchSize", config.getBatchSize());
            status.put("hiddenSize", config.getHiddenSize());
            status.put("inputSize", config.getInputSize());
            
        } catch (Exception e) {
            status.put("error", "Failed to get training status: " + e.getMessage());
            status.put("lastTrainingDate", "Error");
            status.put("daysSinceTraining", -1);
            status.put("needsIncremental", true);
            status.put("modelExists", false);
        }
        
        return status;
    }

    public Map<String, Object> getModelInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            File modelFile = new File(config.getModelFilePath());
            
            info.put("modelExists", modelFile.exists());
            info.put("modelPath", config.getModelFilePath());
            info.put("version", config.getVersion());
            info.put("hiddenSize", config.getHiddenSize());
            info.put("inputSize", config.getInputSize());
            info.put("outputSize", config.getOutputSize());
            info.put("epochs", config.getEpochs());
            info.put("batchSize", config.getBatchSize());
            info.put("trainingRate", config.getTrainingRate());
            
            if (modelFile.exists()) {
                info.put("modelSize", String.format("%.2f MB", modelFile.length() / (1024.0 * 1024.0)));
                info.put("lastModified", new java.util.Date(modelFile.lastModified()).toString());
            }
            
        } catch (Exception e) {
            info.put("error", "Failed to get model info: " + e.getMessage());
        }
        
        return info;
    }

    public String validateModel() {
        try {
            File modelFile = new File(config.getModelFilePath());
            
            if (!modelFile.exists()) {
                return "Model file does not exist at: " + config.getModelFilePath();
            }
            
            LSTMNetwork lstm = LSTMNetwork.loadModel(config.getModelFilePath());
            if (lstm == null) {
                return "Failed to load model from: " + config.getModelFilePath();
            }
            
            if (lstm.getMin() == null || lstm.getMax() == null) {
                return "Model loaded but normalization parameters are missing";
            }
            
            if (lstm.getHiddenSize() != config.getHiddenSize()) {
                return String.format("Model hidden size mismatch. Expected: %d, Found: %d", 
                    config.getHiddenSize(), lstm.getHiddenSize());
            }
            
            return "Model validation successful";
            
        } catch (Exception e) {
            return "Model validation failed: " + e.getMessage();
        }
    }

    private void optimizeMemoryUsage() {
        try {
            epochList.clear();
            accuracyList.clear();
            lossList.clear();
            validationAccuracyList.clear();
            validationLossList.clear();
            
            currentTrainingMessage = "";
            currentEpoch = 0;
            currentProgress = 0.0;
            
            System.gc();
            
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory() / 1024 / 1024;
            long freeMemory = runtime.freeMemory() / 1024 / 1024;
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            
            LOGGER.log(Level.INFO, String.format(
                "Memory optimized - Used: %d MB, Free: %d MB, Total: %d MB, Max: %d MB", 
                usedMemory, freeMemory, totalMemory, maxMemory
            ));
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during memory optimization: " + e.getMessage());
        }
    }

    public Map<String, Object> getCurrentTrainingProgress() {
        Map<String, Object> progress = new HashMap<>();
        progress.put("isTraining", isTraining);
        progress.put("currentMessage", currentTrainingMessage);
        progress.put("currentEpoch", currentEpoch);
        progress.put("totalEpochs", config.getEpochs());
        progress.put("progress", currentProgress);
        progress.put("estimatedTimeRemaining", calculateEstimatedTime());
        return progress;
    }

    private String calculateEstimatedTime() {
        if (!isTraining || currentEpoch == 0) return "Unknown";
        
        long elapsed = System.currentTimeMillis() - trainingStartTime;
        if (currentProgress > 0) {
            long estimated = (long)(elapsed * (100.0 / currentProgress));
            long remaining = estimated - elapsed;
            return formatTime(remaining);
        }
        return "Unknown";
    }

    private String formatTime(long millis) {
        if (millis < 0) return "Complete";
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) return String.format("%dh %dm", hours, minutes % 60);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds % 60);
        return String.format("%ds", seconds);
    }

    private long trainingStartTime;

    private void generateTrainingCharts() {
        try {
            String chartDir = config.getOutputDir() + "/charts" + config.getVersion() + "_" + config.getEpochs() + "/accuracy";
            createDirectory(chartDir);
            
            // Generate accuracy and loss charts
            CustomChartUtils.saveAccuracyChart("Model Accuracy", epochList, accuracyList, validationAccuracyList, 
                chartDir + "/model_accuracy.png", "Epochs", "Accuracy", 10);
            CustomChartUtils.saveLossChart("Model Loss", epochList, lossList, validationLossList, 
                chartDir + "/model_loss.png", "Epochs", "Loss", 10);
                
            System.out.println("Training charts saved to: " + chartDir);
            
        } catch (Exception e) {
            System.err.println("Failed to generate charts: " + e.getMessage());
        }
    }
}
