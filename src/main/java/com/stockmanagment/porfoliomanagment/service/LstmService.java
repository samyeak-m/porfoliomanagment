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

import org.springframework.stereotype.Service;

import com.stockmanagment.porfoliomanagment.config.LstmConfig;
import com.stockmanagment.porfoliomanagment.dto.PredictionResponseDTO;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.database.DatabaseHelper;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.lstm.LSTMNetwork;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.lstm.LSTMTrainer;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.CustomChartUtils;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.DataPreprocessor;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.TechnicalIndicators;

@Service
public class LstmService {
    private static final Logger LOGGER = Logger.getLogger(LstmService.class.getName());
    
    private final LstmConfig config;
    
    private double[] min;
    private double[] max;
    
    // ADD: Class-level LSTM network field
    private LSTMNetwork lstm;

    // Lists to track training progress
    private final List<Integer> epochList = new ArrayList<>();
    private final List<Double> accuracyList = new ArrayList<>();
    private final List<Double> lossList = new ArrayList<>();
    private final List<Double> validationAccuracyList = new ArrayList<>();
    private final List<Double> validationLossList = new ArrayList<>();

    // Add these fields to track current progress
    private volatile String currentTrainingMessage = "";
    private volatile int currentEpoch = 0;
    private volatile double currentProgress = 0.0;
    private volatile boolean isTraining = false;

    public LstmService(LstmConfig config) {
        this.config = config;
    }
    
    public void train() {
        trainingStartTime = System.currentTimeMillis();
        
        try {
            isTraining = true;
            currentTrainingMessage = "Initializing neural network...";
            currentProgress = 0.0;
            
            createDirectory(config.getOutputDir());
            currentTrainingMessage = "Creating output directory completed";
            currentProgress = 5.0;
            
            DatabaseHelper dbHelper = new DatabaseHelper();
            currentTrainingMessage = "Connecting to database...";
            currentProgress = 10.0;
            
            // FIXED: Use class field instead of local variable
            lstm = LSTMNetwork.loadModel(config.getModelFilePath());
            
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
                
                // FIXED: Create new LSTM network using class field
                currentTrainingMessage = "Creating new LSTM network...";
                lstm = new LSTMNetwork(config.getInputSize(), config.getHiddenSize(), 
                                     config.getOutputSize(), config.getDenseSize(), min, max);
                
                currentTrainingMessage = "Starting LSTM training with " + config.getEpochs() + " epochs...";
                
                // FIXED: Pass class field to trainModel
                double[] averages = trainModel(lstm, trainData, testData, config.getEpochs(), config.getTrainingRate(), min, max);
                
                currentTrainingMessage = "Training completed. Saving model...";
                currentProgress = 95.0;
                
                // Save the trained model
                lstm.saveModel(config.getModelFilePath());
                
                // Generate confusion matrix and log results
                int[][] confusionMatrix = lstm.computeConfusionMatrix(testData, testData[testData.length-1][1], config.getThreshold());
                double[][] classMetrics = printConfusionMatrix(confusionMatrix);
                
                // Log training results
                logFile(averages[0], averages[1], averages[0], averages[1], confusionMatrix,
                       classMetrics[1][0], classMetrics[1][1], classMetrics[1][2], // Negative class
                       classMetrics[0][0], classMetrics[0][1], classMetrics[0][2], // Positive class  
                       trainData, testData, testData);
                
                // Generate charts
                generateTrainingCharts();
                
                // Save training completion date
                saveLastTrainingDate();
                
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
            throw new RuntimeException("Training failed: " + e.getMessage(), e);
        }
    }

    // Add method to check if incremental learning is needed
    private boolean shouldPerformIncrementalLearning() {
        try {
            File dateFile = new File(config.getLastTrainingDateFile());
            if (!dateFile.exists()) {
                return true; // First time training
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
            return true; // Default to retraining on error
        }
    }

    // Add method to save training date
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

    // Add incremental learning method
    public void incrementalLearning() {
        try {
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
            
            // Get new data since last training
            List<String> tableNames = dbHelper.getAllStockTableNames();
            List<double[]> newStockData = getNewDataSinceLastTraining(dbHelper, tableNames);
            
            if (newStockData.isEmpty()) {
                System.out.println("No new data available for incremental learning.");
                return;
            }
            
            System.out.println("Found " + newStockData.size() + " new data points for incremental learning");
            
            // Prepare new data
            double[][] newStockDataArray = newStockData.toArray(new double[0][]);
            double[][] technicalIndicators = TechnicalIndicators.calculate(newStockDataArray, 16, 3);
            double[][] extendedData = DataPreprocessor.addFeatures(newStockDataArray, technicalIndicators);
            
            // Use existing min/max for normalization
            min = lstm.getMin();
            max = lstm.getMax();
            
            double[][] normalizedNewData = DataPreprocessor.normalize(extendedData, min, max);
            
            // Perform incremental training
            performIncrementalTraining(lstm, normalizedNewData);
            
            // Save updated model
            lstm.saveModel(config.getModelFilePath());
            saveLastTrainingDate();
            
            System.out.println("Incremental learning completed successfully!");
            
        } catch (Exception e) {
            throw new RuntimeException("Incremental learning failed: " + e.getMessage(), e);
        }
    }

    // Add method to get new data since last training
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
            
            // Get data after last training date for each stock
            for (String tableName : tableNames) {
                List<double[]> stockData = dbHelper.loadStockDataAfterDate(tableName, lastTrainingDate);
                newData.addAll(stockData);
            }
            
        } catch (Exception e) {
            System.err.println("Error getting new data: " + e.getMessage());
        }
        
        return newData;
    }

    // Add method to perform incremental training
    private void performIncrementalTraining(LSTMNetwork lstm, double[][] newData) {
        System.out.println("Performing incremental training on " + newData.length + " new samples");
        
        // Use lower learning rate for incremental learning
        double incrementalLearningRate = config.getTrainingRate() * 0.1; // 10% of original learning rate
        int incrementalEpochs = 5; // Fewer epochs for incremental learning
        
        LSTMTrainer trainer = new LSTMTrainer(lstm, incrementalLearningRate);
        
        for (int epoch = 0; epoch < incrementalEpochs; epoch++) {
            System.out.println("Incremental epoch " + (epoch + 1) + "/" + incrementalEpochs);
            
            // Shuffle data for each epoch
            List<double[]> dataList = Arrays.asList(newData);
            Collections.shuffle(dataList);
            double[][] shuffledData = dataList.toArray(new double[0][]);
            
            for (double[] data : shuffledData) {
                double[] hiddenState = new double[lstm.getHiddenSize()];
                double[] cellState = new double[lstm.getHiddenSize()];
                
                double[] input = Arrays.copyOf(data, config.getInputSize());
                double[] target = new double[]{data[data.length - 1]};
                
                // Forward pass
                double[] output = lstm.forward(input, hiddenState, cellState);
                if (output == null) {
                    continue;
                }
                
                // Backward pass with lower learning rate
                lstm.backpropagate(input, target, incrementalLearningRate);
            }
            
            // Calculate and log incremental training metrics
            double accuracy = calculateIncrementalAccuracy(lstm, shuffledData);
            System.out.println("Incremental Epoch " + epoch + " Accuracy: " + String.format("%.4f", accuracy));
        }
    }

    // Add method to calculate incremental accuracy
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
        int tp = matrix[0][0]; // True Positives
        int fn = matrix[1][0]; // False Negatives
        int fp = matrix[0][1]; // False Positives
        int tn = matrix[1][1]; // True Negatives

        // Positive Class Metrics
        double precisionPositive = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0;
        double recallPositive = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0;
        double f1ScorePositive = (precisionPositive + recallPositive) > 0 ? 2 * (precisionPositive * recallPositive) / (precisionPositive + recallPositive) : 0;

        // Negative Class Metrics
        double precisionNegative = (tn + fn) > 0 ? (double) tn / (tn + fn) : 0;
        double recallNegative = (tn + fp) > 0 ? (double) tn / (tn + fp) : 0;
        double f1ScoreNegative = (precisionNegative + recallNegative) > 0 ? 2 * (precisionNegative * recallNegative) / (precisionNegative + recallNegative) : 0;

        // Print Confusion Matrix
        System.out.println("Confusion Matrix:");
        System.out.println("TP: " + tp + ", FN: " + fn);
        System.out.println("FP: " + fp + ", TN: " + tn);

        // Print Positive Class Metrics
        System.out.println("Positive Class:");
        System.out.println("Precision: " + String.format("%.4f", precisionPositive));
        System.out.println("Recall: " + String.format("%.4f", recallPositive));
        System.out.println("F1 Score: " + String.format("%.4f", f1ScorePositive));

        // Print Negative Class Metrics
        System.out.println("Negative Class:");
        System.out.println("Precision: " + String.format("%.4f", precisionNegative));
        System.out.println("Recall: " + String.format("%.4f", recallNegative));
        System.out.println("F1 Score: " + String.format("%.4f", f1ScoreNegative));

        // Return the results as a 2D array for both classes (positive and negative)
        return new double[][]{
                {precisionPositive, recallPositive, f1ScorePositive}, // Positive class
                {precisionNegative, recallNegative, f1ScoreNegative}  // Negative class
        };
    }

    private double[] trainModel(LSTMNetwork lstm, double[][] trainData, double[][] validationData, int epochs, double learningRate, double[] min, double[] max) {
        LSTMTrainer trainer = new LSTMTrainer(lstm, learningRate);
        double totalAccuracy = 0;
        double totalLoss = 0;

        for (int epoch = 0; epoch < epochs; epoch++) {
            currentEpoch = epoch + 1;
            currentTrainingMessage = "Training epoch " + (epoch + 1) + "/" + epochs + " - Learning patterns...";
            
            // Calculate progress: 60% to 95% during training
            currentProgress = 60.0 + (35.0 * (epoch + 1) / epochs);
            
            long startTime = System.currentTimeMillis();
            
            int totalDataPoints = trainData.length;
            int batchSize = config.getBatchSize();
            int batches = totalDataPoints / batchSize;

            double totalEpochLoss = 0;

            for (int batch = 0; batch < batches; batch++) {
                double[][] batchData = Arrays.copyOfRange(trainData, batch * batchSize, (batch + 1) * batchSize);
                for (double[] data : batchData) {
                    double[] hiddenState = new double[lstm.getHiddenSize()];
                    double[] cellState = new double[lstm.getHiddenSize()];
                    
                    double[] input = Arrays.copyOf(data, config.getInputSize());
                    double[] target = new double[]{data[data.length - 1]};
                    
                    // CORRECT ORDER: Forward first, then backprop
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
            String elapsedTime = String.format("%02d:%02d:%02d",
                    (elapsedTimeMillis / (1000 * 60 * 60)) % 24,
                    (elapsedTimeMillis / (1000 * 60)) % 60,
                    (elapsedTimeMillis / 1000) % 60);

            epochList.add(epoch);
            accuracyList.add(accuracy);
            lossList.add(epochLoss);
            validationAccuracyList.add(validationAccuracy);
            validationLossList.add(validationLoss);

            logTrainingProgress(epoch, accuracy, epochLoss, validationAccuracy, validationLoss, elapsedTimeMillis);

            totalAccuracy += accuracy;
            totalLoss += epochLoss;
            
            // Update message with current metrics
            currentTrainingMessage = String.format(
                "Epoch %d/%d - Acc: %.3f, Loss: %.3f, Val_Acc: %.3f, Val_Loss: %.3f", 
                epoch + 1, epochs, accuracy, epochLoss, validationAccuracy, validationLoss
            );

            // Add sleep to make progress visible (optional)
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
    
    // Log to file for debugging
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

            // Apply consistent constraints
            prediction = applyPredictionConstraints(prediction, currentClosePrice);

            double accuracy = calculatePredictionAccuracy(prediction, actual, currentClosePrice);
            totalAccuracy += accuracy;

            // In testModel, add debug output
            if (i < 5) { // Debug first 5 predictions
                System.out.printf("Raw prediction: %.4f, Last close: %.4f, After constraints: %.4f, Actual: %.4f%n", 
                    output[0], currentClosePrice, prediction, actual);
            }
        }
        return totalAccuracy / (testData.length - 1);
    }

    private double applyPredictionConstraints(double prediction, double lastClosePrice) {
        // More realistic daily change limits (5% instead of 8%)
        double maxDailyChange = 0.05; // 5% max daily change
        double minPrice = lastClosePrice * (1 - maxDailyChange);
        double maxPrice = lastClosePrice * (1 + maxDailyChange);

        // Apply realistic constraints
        if (prediction < minPrice) {
            prediction = minPrice;
        } else if (prediction > maxPrice) {
            prediction = maxPrice;
        }

        return prediction;
    }

    private double calculatePredictionAccuracy(double prediction, double actual, double currentClosePrice) {
        double maxChange = 0.05 * currentClosePrice; // Match the constraint limit
        double diff = Math.abs(prediction - actual);

        if (diff > maxChange) {
            return 0;
        }

        double accuracy = 1 - (diff / maxChange);
        return accuracy;
    }

    private double calculateLoss(LSTMNetwork lstm, double[][] data) {
        double totalLoss = 0;
        double maxChange = 0.05; // Match the constraint limit

        for (int i = 0; i < data.length - 1; i++) {
            double[] input = Arrays.copyOf(data[i], config.getInputSize());
            checkForNaN1D(input, "input to calculateLoss");
            
            double lastClosePrice = data[i][1];
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            
            if (output == null) {
                continue;
            }
            
            double prediction = output[0];

            // Apply the same constraints as in applyPredictionConstraints
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
        
            // Model Configuration
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
            
            // Dataset Information
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

            // Model Performance
            writer.write("=== MODEL PERFORMANCE ===\n");
            writer.write("Final Test Accuracy: " + String.format("%.4f", finalTestAccuracy) + "\n");
            writer.write("Final Test Loss: " + String.format("%.4f", finalTestLoss) + "\n");
            writer.write("Overall Average Training Accuracy: " + String.format("%.4f", averageAccuracy) + "\n");
            writer.write("Overall Average Training Loss: " + String.format("%.4f", averageLoss) + "\n");
            writer.write("\n");

            // Confusion Matrix
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

            // Class Metrics
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

            // Overall Metrics
            double accuracy = (double)(tp + tn) / total;
            double errorRate = (double)(fp + fn) / total;
            
            writer.write("=== OVERALL METRICS ===\n");
            writer.write("Overall Accuracy: " + String.format("%.4f", accuracy) + " (" + (tp + tn) + "/" + total + ")\n");
            writer.write("Overall Error Rate: " + String.format("%.4f", errorRate) + " (" + (fp + fn) + "/" + total + ")\n");
            writer.write("\n");

            // Technical Indicators Summary
            writer.write("=== TECHNICAL INDICATORS USED ===\n");
            writer.write("1. EMA (Exponential Moving Average) - Period: 16\n");
            writer.write("2. SMA (Simple Moving Average) - Period: 20\n");
            writer.write("3. RSI (Relative Strength Index) - Period: 3\n");
            writer.write("4. ATR (Average True Range) - Period: 14\n");
            writer.write("5. MACD (Moving Average Convergence Divergence) - Periods: 12,26,9\n");
            writer.write("6. Bollinger Bands - Period: 20, Std Dev: 2.0\n");
            writer.write("7. Stochastic Oscillator - Period: 14\n");
            writer.write("\n");

            // Sample Technical Indicators Output
            writer.write("=== TECHNICAL INDICATORS CALCULATION DETAILS ===\n");
            writer.write("Calculation Method: Dynamic from actual training data\n");
            writer.write("Data Source: First " + Math.min(5, trainData.length) + " rows of training dataset\n");
            writer.write("EMA Period: 16, SMA Period: 20, RSI Period: 3\n");
            writer.write("MACD Periods: 12,26,9 | BB Period: 20, StdDev: 2.0 | Stochastic Period: 14\n");
            writer.write("\n");

            writer.write("=== SAMPLE TECHNICAL INDICATORS (First 5 Rows) ===\n");

            if (trainData.length > 0) {
                try {
                    // Get sample of original stock data for indicator calculation
                    int sampleRows = Math.min(5, trainData.length);
                    
                    // Create sample data array with proper structure
                    double[][] sampleData = new double[sampleRows][6];
                    for (int i = 0; i < sampleRows; i++) {
                        sampleData[i][0] = trainData[i][0]; // table identifier
                        sampleData[i][1] = denormalizeValue(trainData[i][1], min[1], max[1]); // close price
                        sampleData[i][2] = denormalizeValue(trainData[i][2], min[2], max[2]); // high price
                        sampleData[i][3] = denormalizeValue(trainData[i][3], min[3], max[3]); // low price
                        sampleData[i][4] = denormalizeValue(trainData[i][4], min[4], max[4]); // open price
                        sampleData[i][5] = denormalizeValue(trainData[i][5], min[5], max[5]); // date
                    }
                    
                    // Calculate technical indicators
                    double[][] indicators = TechnicalIndicators.calculate(sampleData, 16, 3);
                    
                    // Output calculated indicators
                    for (int i = 0; i < sampleRows; i++) {
                        writer.write(String.format(
                            "Row %d: Close=%.2f, EMA=%.2f, SMA=%.2f, RSI=%.2f, ATR=%.2f, MACD=%.2f, Signal=%.2f, Histogram=%.2f, BB_Upper=%.2f, BB_Lower=%.2f, Stoch_K=%.2f, Stoch_D=%.2f%n",
                            i,
                            sampleData[i][1],         // Original close price
                            indicators[i][0],         // EMA
                            indicators[i][1],         // SMA  
                            indicators[i][2],         // RSI
                            indicators[i][3],         // ATR
                            indicators[i][4],         // MACD
                            indicators[i][5],         // Signal
                            indicators[i][6],         // Histogram
                            indicators[i][8],         // BB_Upper
                            indicators[i][9],         // BB_Lower
                            indicators[i][10],        // Stoch_K
                            indicators[i][11]         // Stoch_D
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

            // Training Epoch Details
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

        // TEMPORARILY DISABLE BALANCING FOR TESTING
        // trainData = balanceDataset(trainData);
        // testData = balanceDataset(testData);
        
        System.out.println("Using original unbalanced data for testing");
        
        min = DataPreprocessor.calculateMin(data); // Use original data for min/max
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

    private double[][] balanceDataset(double[][] data) {
        List<double[]> positiveClass = new ArrayList<>();
        List<double[]> negativeClass = new ArrayList<>();
        
        System.out.println("Original data size: " + data.length);
        
        for (int i = 0; i < data.length - 1; i++) {
            double currentPrice = data[i][1];  // close price
            double nextPrice = data[i + 1][1]; // next day's close price
            
            // Calculate percentage change
            double priceChange = (nextPrice - currentPrice) / currentPrice;
            
            // Use smaller, more realistic thresholds
            if (priceChange > 0.001) { // Up more than 0.1%
                positiveClass.add(data[i]);
            } else if (priceChange < -0.001) { // Down more than 0.1%
                negativeClass.add(data[i]);
            }
            // Skip samples with very small changes (-0.1% to +0.1%)
        }
        
        System.out.println("Positive samples: " + positiveClass.size());
        System.out.println("Negative samples: " + negativeClass.size());
        
        // Use the smaller class size for both
        int minSize = Math.min(positiveClass.size(), negativeClass.size());
        
        if (minSize < 100) {
            System.err.println("Warning: Small balanced dataset size: " + minSize);
            System.err.println("Using original data without balancing");
            return data; // Return original data if balancing creates too small dataset
        }
        
        List<double[]> balancedData = new ArrayList<>();
        balancedData.addAll(positiveClass.subList(0, minSize));
        balancedData.addAll(negativeClass.subList(0, minSize));
        
        System.out.println("Balanced dataset: " + minSize + " positive, " + minSize + " negative samples");
        
        return balancedData.toArray(new double[0][]);
    }

    private void checkForNaN(double[][] data, String label) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (Double.isNaN(data[i][j]) || Double.isInfinite(data[i][j])) {
                    System.err.println("Invalid value in " + label + " at [" + i + "][" + j + "]: " + data[i][j]);
                }
            }
        }
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

    /**
     * Denormalize a single value from [0,1] range back to original scale
     */
    private double denormalizeValue(double normalizedValue, double minValue, double maxValue) {
        return normalizedValue * (maxValue - minValue) + minValue;
    }

    /**
     * Get actual technical indicators from processed training data
     */
    private double[][] getActualTechnicalIndicators(double[][] normalizedTrainData, int sampleSize) {
        try {
            int samples = Math.min(sampleSize, normalizedTrainData.length);
            double[][] denormalizedData = new double[samples][6];
            
            // Denormalize the data back to original scale
            for (int i = 0; i < samples; i++) {
                denormalizedData[i][0] = normalizedTrainData[i][0]; // table identifier (not normalized)
                denormalizedData[i][1] = denormalizeValue(normalizedTrainData[i][1], min[1], max[1]); // close
                denormalizedData[i][2] = denormalizeValue(normalizedTrainData[i][2], min[2], max[2]); // high
                denormalizedData[i][3] = denormalizeValue(normalizedTrainData[i][3], min[3], max[3]); // low
                denormalizedData[i][4] = denormalizeValue(normalizedTrainData[i][4], min[4], max[4]); // open
                denormalizedData[i][5] = denormalizeValue(normalizedTrainData[i][5], min[5], max[5]); // date
            }
            
            // Calculate fresh technical indicators
            return TechnicalIndicators.calculate(denormalizedData, 16, 3);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating actual technical indicators", e);
            return new double[0][0];
        }
    }

    // Update your predict method to load the model
    public PredictionResponseDTO predict(String stockSymbol) {
        try {
            // FIXED: Load the trained model
            lstm = LSTMNetwork.loadModel(config.getModelFilePath());
            if (lstm == null) {
                throw new RuntimeException("No trained model found. Please train the model first.");
            }
            
            // Get min/max from the loaded model
            min = lstm.getMin();
            max = lstm.getMax();
            
            if (min == null || max == null) {
                throw new RuntimeException("Model normalization parameters not found. Please retrain the model.");
            }
            
            // FIXED: Convert to lowercase for database operations
            String normalizedStockSymbol = stockSymbol.toLowerCase();
            
            DatabaseHelper dbHelper = new DatabaseHelper();
            List<double[]> stockData = dbHelper.loadStockData(normalizedStockSymbol);
            
            if (stockData.isEmpty()) {
                throw new RuntimeException("No data found for stock symbol: " + stockSymbol);
            }
            
            double[][] stockDataArray = stockData.toArray(new double[0][]);
            double[][] technicalIndicators = TechnicalIndicators.calculate(stockDataArray, 16, 3);
            double[][] extendedData = DataPreprocessor.addFeatures(stockDataArray, technicalIndicators);
            
            // Store original last close price BEFORE normalization
            double originalLastClose = extendedData[extendedData.length - 1][1];
            
            extendedData = DataPreprocessor.normalize(extendedData, min, max);

            double[] input = Arrays.copyOf(extendedData[extendedData.length - 1], config.getInputSize());
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            
            if (output == null) {
                throw new RuntimeException("LSTM forward pass returned null");
            }
            
            double prediction = output[0];
            
            // FIXED: Denormalize the prediction back to original scale
            double denormalizedPrediction = prediction * (max[1] - min[1]) + min[1];
            
            // Use denormalized prediction for constraints
            denormalizedPrediction = applyPredictionConstraints(denormalizedPrediction, originalLastClose);

            double pointChange = denormalizedPrediction - originalLastClose;
            double priceChange = (pointChange / originalLastClose) * 100;

            PredictionResponseDTO response = new PredictionResponseDTO();
            response.setStockSymbol(stockSymbol.toUpperCase()); // Display in uppercase
            response.setPrediction(denormalizedPrediction);
            response.setLastClose(originalLastClose);
            response.setPointChange(pointChange);
            response.setPriceChange(priceChange);
            response.setPredictionDate(LocalDate.now().toString());

            return response;
        } catch (Exception e) {
            System.err.println("Prediction failed for " + stockSymbol + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
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
            
            // Check if model exists
            File modelFile = new File(config.getModelFilePath());
            boolean modelExists = modelFile.exists();
            
            status.put("lastTrainingDate", lastTrainingDate != null ? lastTrainingDate.toString() : "Never");
            status.put("daysSinceTraining", daysSinceTraining);
            status.put("needsIncremental", needsIncremental);
            status.put("modelExists", modelExists);
            status.put("retrainThresholdDays", config.getRetrainThresholdDays());
            status.put("currentDate", LocalDate.now().toString());
            
            // Add model info if it exists
            if (modelExists) {
                status.put("modelPath", config.getModelFilePath());
                status.put("modelSize", String.format("%.2f MB", modelFile.length() / (1024.0 * 1024.0)));
            }
            
            // Add training parameters
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

    private void validateModelInputs(double[] input, String context) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null in " + context);
        }
        
        if (input.length != config.getInputSize()) {
            throw new IllegalArgumentException(
                String.format("Input size mismatch in %s. Expected: %d, Got: %d", 
                    context, config.getInputSize(), input.length));
        }
        
        for (int i = 0; i < input.length; i++) {
            if (!Double.isFinite(input[i])) {
                throw new IllegalArgumentException(
                    String.format("Invalid input value at index %d in %s: %f", 
                        i, context, input[i]));
            }
        }
    }

    private void optimizeMemoryUsage() {
        // Clear training lists after chart generation
        epochList.clear();
        accuracyList.clear();
        lossList.clear();
        validationAccuracyList.clear();
        validationLossList.clear();
        
        // Force garbage collection
        System.gc();
        
        // Log memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Memory usage: " + (usedMemory / 1024 / 1024) + " MB");
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
        
        // Simple estimation based on current progress
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

    // Add training start time tracking
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
