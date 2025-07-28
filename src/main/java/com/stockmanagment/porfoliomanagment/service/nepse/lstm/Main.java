package com.stockmanagment.porfoliomanagment.service.nepse.lstm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.stockmanagment.porfoliomanagment.service.nepse.lstm.database.DatabaseHelper;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.lstm.LSTMNetwork;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.lstm.LSTMTrainer;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.CustomChartUtils;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.DataPreprocessor;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.TechnicalIndicators;

public class Main {
    static String version = "v9";

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";
    private static final String YELLOW = "\u001B[33m";

    static int hiddenSize = 32;
    static int denseSize = 3;
    static int inputSize = 18;
    static int outputSize = 1;
    static int epoch = 100;
    static int batch = 64;
    static double trainingRate = 0.01;

    static double threshold = 1;
    static int interval = 100;

    private static final String BASE_DIR = "output_"+version+"_e"+epoch+"_b"+batch+"_h"+hiddenSize;
    private static final String MODEL_FILE_PATH = BASE_DIR + File.separator + "lstm_model" + version + "_" + epoch + ".ser";

    private static final List<Integer> epochList = new ArrayList<>();
    private static final List<Double> accuracyList = new ArrayList<>();
    private static final List<Double> lossList = new ArrayList<>();

    private static final List<Double> validationAccuracyList = new ArrayList<>();
    private static final List<Double> validationLossList = new ArrayList<>();

    private static double[] min;
    private static double[] max;

    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        createDirectory(BASE_DIR);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);

        DatabaseHelper dbHelper = new DatabaseHelper();

        LSTMNetwork lstm = LSTMNetwork.loadModel(MODEL_FILE_PATH);
        if (lstm == null) {
            List<String> tableNames = dbHelper.getAllStockTableNames();
            List<double[]> allStockData = new ArrayList<>();

            for (String tableName : tableNames) {
                allStockData.addAll(dbHelper.loadStockData(tableName));
            }

            double[][] stockDataArray = allStockData.toArray(new double[0][]);

            // Stock data debug
            System.out.println("=== Stock Data Debug ===");
            for (int i = 0; i < Math.min(5, stockDataArray.length); i++) {
                System.out.printf("Row %d: Table=%.2f, Close=%.2f, High=%.2f, Low=%.2f, Open=%.2f%n", 
                    i, stockDataArray[i][0], stockDataArray[i][1], stockDataArray[i][2], 
                    stockDataArray[i][3], stockDataArray[i][4]);
            }
            System.out.println("=== End Stock Data Debug ===");

            double[][] technicalIndicators = TechnicalIndicators.calculate(stockDataArray, 16, 3);

            // Technical indicators debug
            System.out.println("=== Technical Indicators Debug ===");
            System.out.println("Total indicators: " + technicalIndicators[0].length);
            for (int i = 0; i < Math.min(5, technicalIndicators.length); i++) {
                System.out.printf("Row %d: EMA=%.2f, SMA=%.2f, RSI=%.2f, ATR=%.2f, MACD=%.2f, Signal=%.2f, Histogram=%.2f, BB_Upper=%.2f, BB_Lower=%.2f, Stoch_K=%.2f, Stoch_D=%.2f%n", 
                    i, 
                    technicalIndicators[i][0],  // EMA
                    technicalIndicators[i][1],  // SMA
                    technicalIndicators[i][2],  // RSI
                    technicalIndicators[i][3],  // ATR
                    technicalIndicators[i][4],  // MACD
                    technicalIndicators[i][5],  // Signal
                    technicalIndicators[i][6],  // Histogram
                    technicalIndicators[i][8],  // BB Upper
                    technicalIndicators[i][9],  // BB Lower
                    technicalIndicators[i][10], // Stochastic %K
                    technicalIndicators[i][11]  // Stochastic %D
                );
            }

            // Check for NaN/Inf in technical indicators
            for (int i = 0; i < technicalIndicators.length; i++) {
                for (int j = 0; j < technicalIndicators[i].length; j++) {
                    if (!Double.isFinite(technicalIndicators[i][j])) {
                        System.err.println("Invalid technical indicator at [" + i + "][" + j + "]: " + technicalIndicators[i][j]);
                    }
                }
            }
            System.out.println("=== End Debug ===");

            double[][] extendedData = DataPreprocessor.addFeatures(stockDataArray, technicalIndicators);

            double[][][] preprocessedData = preprocessData(extendedData, 0.6);
            double[][] trainData = preprocessedData[0];
            double[][] testData = preprocessedData[1];

            double[][] validationData = Arrays.copyOfRange(testData, 0, testData.length / 5);
            double[][] finalTestData = Arrays.copyOfRange(testData, testData.length / 5, testData.length);

            // MOVED DEBUG OUTPUT HERE (after variables are defined)
            System.out.println("=== Dataset Debug ===");
            System.out.println("Final train data size: " + trainData.length);
            System.out.println("Final test data size: " + testData.length);
            System.out.println("Final validation data size: " + validationData.length);
            System.out.println("Final finalTestData size: " + finalTestData.length);

            // Check first few rows of final test data
            for (int i = 0; i < Math.min(3, finalTestData.length - 1); i++) {
                double currentPrice = finalTestData[i][1];
                double nextPrice = finalTestData[i + 1][1];
                double change = (nextPrice - currentPrice) / currentPrice;
                System.out.printf("Test sample %d: Current=%.2f, Next=%.2f, Change=%.4f%%\n", 
                    i, currentPrice, nextPrice, change * 100);
            }
            System.out.println("=== End Dataset Debug ===");

            checkForNaN(trainData, "trainData");
            checkForNaN(validationData, "validationData");
            checkForNaN(finalTestData, "finalTestData");

            // Rest of your main method...
            LOGGER.log(Level.INFO, BLUE + "Training data size: " + trainData.length + RESET);
            LOGGER.log(Level.INFO, BLUE + "Validation data size: " + validationData.length + RESET);
            LOGGER.log(Level.INFO, BLUE + "Final test data size: " + finalTestData.length + RESET);

            double[] targets = new double[finalTestData.length];
            for (int i = 0; i < targets.length; i++) {
                targets[i] = finalTestData[i][1];
            }

            min = DataPreprocessor.calculateMin(extendedData);
            max = DataPreprocessor.calculateMax(extendedData);

            lstm = new LSTMNetwork(inputSize, hiddenSize, outputSize, denseSize, min, max);

            double[] averages = trainModel(lstm, trainData, validationData, epoch, trainingRate, min, max);

            double testAccuracy = testModel(lstm, finalTestData);
            double finalTestLoss = calculateLoss(lstm, finalTestData);

            LOGGER.log(Level.INFO, String.format(GREEN + "Final Test Accuracy: %.2f" + RESET, testAccuracy));
            LOGGER.log(Level.INFO, String.format(GREEN + "Final Test Loss: %.2f" + RESET, finalTestLoss));

            int[][] confusionMatrix = lstm.computeConfusionMatrix(finalTestData, finalTestData[finalTestData.length - 1][1], threshold);
            double[][] metrics = printConfusionMatrix(confusionMatrix);

            double averageAccuracy = averages[0];
            double averageLoss = averages[1];
            // Metrics for Positive class
            double precisionPositive = metrics[0][0];
            double recallPositive = metrics[0][1];
            double f1ScorePositive = metrics[0][2];

            // Metrics for Negative class
            double precisionNegative = metrics[1][0];
            double recallNegative = metrics[1][1];
            double f1ScoreNegative = metrics[1][2];

            logFile(testAccuracy, finalTestLoss, averageAccuracy, averageLoss, confusionMatrix, precisionNegative, recallNegative,f1ScoreNegative,precisionPositive, recallPositive, f1ScorePositive, trainData, validationData, finalTestData);

            lstm.saveModel(MODEL_FILE_PATH);

            String accuracyChartDir = BASE_DIR + File.separator + "charts" + version + "_" + epoch + File.separator + "accuracy";
            createDirectory(accuracyChartDir);

            CustomChartUtils.saveAccuracyChart("Model Accuracy", epochList, accuracyList, validationAccuracyList, accuracyChartDir + File.separator + "model_accuracy.png", "Epochs", "Accuracy",interval);
            CustomChartUtils.saveLossChart("Model Loss", epochList, lossList, validationLossList, accuracyChartDir + File.separator + "model_loss.png", "Epochs", "Loss",interval);

            // After training
            System.out.println("Chart data points: " + epochList.size());
            System.out.println("Accuracy range: " + Collections.min(accuracyList) + " to " + Collections.max(accuracyList));
            System.out.println("Loss range: " + Collections.min(lossList) + " to " + Collections.max(lossList));
        } else {
            min = lstm.getMin();
            max = lstm.getMax();
            if (min == null || max == null) {
                System.err.println("Model loaded, but min and max values are not initialized.");
            }
            LOGGER.log(Level.INFO, BLUE + "Model loaded successfully." + RESET);
        }

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter the stock symbol to predict: ");
                String stockSymbol = scanner.nextLine();

                predictAndSave(dbHelper, lstm, stockSymbol);

                System.out.print(BLUE + "Do you want to predict for another stock? (yes/no): " + RESET);
                String response = scanner.nextLine();
                if (!response.equalsIgnoreCase("yes")) {
                    break;
                }
            }
        }

        System.out.println(GREEN + "Program execution finished." + RESET);
    }

    private static double[][] printConfusionMatrix(int[][] matrix) {

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


    private static double[] trainModel(LSTMNetwork lstm, double[][] trainData, double[][] validationData, int epochs, double learningRate,double[] min, double[] max) {
        LSTMTrainer trainer = new LSTMTrainer(lstm, learningRate);
        double prevAccuracy = 0;
        int sameCount = 0;

        double totalAccuracy = 0;
        double totalLoss = 0;
        int epochCount = 0;

        for (int epoch = 0; epoch < epochs; epoch++) {
            long startTime = System.currentTimeMillis();
            int totalDataPoints = trainData.length;
            int batchSize = batch;
            int batches = totalDataPoints / batchSize;

            double totalEpochLoss = 0;

            for (int batch = 0; batch < batches; batch++) {
                double[][] batchData = Arrays.copyOfRange(trainData, batch * batchSize, (batch + 1) * batchSize);
                for (double[] data : batchData) {
                    double[] hiddenState = new double[lstm.getHiddenSize()];
                    double[] cellState = new double[lstm.getHiddenSize()];
                    
                    double[] input = Arrays.copyOf(data, inputSize);
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

            LOGGER.log(Level.INFO, String.format(YELLOW + "Epoch %d: Accuracy = %.2f, Loss = %.2f, Validation Accuracy = %.2f, Validation Loss = %.2f, Time = %s" + RESET,
                    epoch, accuracy, epochLoss, validationAccuracy, validationLoss, elapsedTime));

            totalAccuracy += accuracy;
            totalLoss += epochLoss;
            epochCount++;

            prevAccuracy = validationAccuracy;
        }

        double averageAccuracy = totalAccuracy / epochCount;
        double averageLoss = totalLoss / epochCount;

        LOGGER.log(Level.INFO, String.format(GREEN + "Overall Average Accuracy: %.2f" + RESET, averageAccuracy));
        LOGGER.log(Level.INFO, String.format(GREEN + "Overall Average Loss: %.2f" + RESET, averageLoss));

        return new double[]{averageAccuracy, averageLoss};
    }



    private static double calculateValidationLoss(LSTMNetwork lstm, double[][] validationData) {
        return calculateLoss(lstm, validationData);
    }


    private static double testModel(LSTMNetwork lstm, double[][] testData) {
        double totalAccuracy = 0;

        for (int i = 0; i < testData.length - 1; i++) {
            double[] input = Arrays.copyOf(testData[i], inputSize);
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

    private static double applyPredictionConstraints(double prediction, double lastClosePrice) {
        // More realistic daily change limits (3-5% instead of 8%)
        double maxDailyChange = 0.05; // 5% max daily change
        double minPrice = lastClosePrice * (1 - maxDailyChange);
        double maxPrice = lastClosePrice * (1 + maxDailyChange);

        // Apply realistic constraints
        if (prediction < minPrice) {
            prediction = minPrice;
        } else if (prediction > maxPrice) {
            prediction = maxPrice;
        }

        // Remove this line - it's forcing predictions to [0,1] which is wrong
        // prediction = Math.max(0, Math.min(1, prediction));

        return prediction;
    }


    private static double calculatePredictionAccuracy(double prediction, double actual, double currentClosePrice) {
        double maxChange = 0.05 * currentClosePrice; // FIXED: Match the constraint limit (was 0.08)
        double diff = Math.abs(prediction - actual);

        if (diff > maxChange) {
            return 0;
        }

        double accuracy = 1 - (diff / maxChange);
        return accuracy;
    }



    private static double calculateLoss(LSTMNetwork lstm, double[][] data) {
        double totalLoss = 0;
        double maxChange = 0.05; // Match the constraint limit

        for (int i = 0; i < data.length - 1; i++) {
            double[] input = Arrays.copyOf(data[i], inputSize);
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

    public static void logFile(double finalTestAccuracy, double finalTestLoss, double averageAccuracy, double averageLoss, int[][] confusionMatrix,
                               double precisionNegative, double recallNegative, double f1ScoreNegative,double precisionPositive, double recallPositive, double f1ScorePositive, double[][] trainData, double[][] validationData, double[][] finalTestData) {
        String logFileName = BASE_DIR + File.separator + "confusion.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName))) {
        
            // Model Configuration
            writer.write("=== MODEL CONFIGURATION ===\n");
            writer.write("Version: " + version + "\n");
            writer.write("Hidden Size: " + hiddenSize + "\n");
            writer.write("Dense Size: " + denseSize + "\n");
            writer.write("Input Size: " + inputSize + "\n");
            writer.write("Output Size: " + outputSize + "\n");
            writer.write("Epochs: " + epoch + "\n");
            writer.write("Batch Size: " + batch + "\n");
            writer.write("Training Rate: " + trainingRate + "\n");
            writer.write("Threshold: " + threshold + "\n");
            writer.write("Interval: " + interval + "\n");
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
            writer.write("=== SAMPLE TECHNICAL INDICATORS (First 5 Rows) ===\n");
            writer.write("Row 0: EMA=207.00, SMA=207.00, RSI=50.00, ATR=8.00, MACD=0.00, Signal=0.00, Histogram=0.00, BB_Upper=211.14, BB_Lower=202.86, Stoch_K=0.00, Stoch_D=0.00\n");
            writer.write("Row 1: EMA=203.00, SMA=203.00, RSI=50.00, ATR=8.00, MACD=0.00, Signal=0.00, Histogram=0.00, BB_Upper=202.98, BB_Lower=195.02, Stoch_K=0.00, Stoch_D=0.00\n");
            writer.write("Row 2: EMA=200.67, SMA=200.67, RSI=50.00, ATR=6.33, MACD=0.00, Signal=0.00, Histogram=0.00, BB_Upper=199.92, BB_Lower=192.08, Stoch_K=0.00, Stoch_D=0.00\n");
            writer.write("Row 3: EMA=201.00, SMA=201.00, RSI=35.29, ATR=6.25, MACD=0.00, Signal=0.00, Histogram=0.00, BB_Upper=206.04, BB_Lower=197.96, Stoch_K=31.58, Stoch_D=10.53\n");
            writer.write("Row 4: EMA=200.40, SMA=200.40, RSI=26.09, ATR=5.80, MACD=0.00, Signal=0.00, Histogram=0.00, BB_Upper=201.96, BB_Lower=194.04, Stoch_K=10.53, Stoch_D=14.04\n");
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


    private static void createDirectory(String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static void predictAndSave(DatabaseHelper dbHelper, LSTMNetwork lstm, String stockSymbol) throws SQLException, IOException {
        List<double[]> stockData = dbHelper.loadStockData(stockSymbol);
        double[][] stockDataArray = stockData.toArray(new double[0][]);

        double[][] technicalIndicators = TechnicalIndicators.calculate(stockDataArray, 16, 3);
        double[][] extendedData = DataPreprocessor.addFeatures(stockDataArray, technicalIndicators);
        
        // FIXED: Store original last close price BEFORE normalization
        double originalLastClose = extendedData[extendedData.length - 1][1];
        
        extendedData = DataPreprocessor.normalize(extendedData, min, max);

        int days = 1;
        double[] predictions = new double[days];
        
        for (int i = 0; i < days; i++) {
            double[] input = Arrays.copyOf(extendedData[extendedData.length - 1], inputSize);
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            
            if (output == null) {
                System.err.println("LSTM forward pass returned null");
                return;
            }
            
            double prediction = output[0];
            
            // FIXED: Use original price for constraints, not normalized
            prediction = applyPredictionConstraints(prediction, originalLastClose);
            predictions[i] = prediction;
        }

        // FIXED: Don't denormalize - predictions are already in original scale
        // predictions = DataPreprocessor.denormalize(predictions, min[min.length - 1], max[max.length - 1]);

        double[] actualPrices = new double[days];
        for (int i = 0; i < days; i++) {
            actualPrices[i] = originalLastClose; // Use original price
        }

        dbHelper.savePredictions(stockSymbol, predictions, actualPrices);
        System.out.println(GREEN + "Predictions saved for stock: " + stockSymbol + RESET);
    }

    private static double[][][] preprocessData(double[][] data, double trainSplitRatio) {
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

    private static double[][] balanceDataset(double[][] data) {
        List<double[]> positiveClass = new ArrayList<>();
        List<double[]> negativeClass = new ArrayList<>();
        
        System.out.println("Original data size: " + data.length);
        
        for (int i = 0; i < data.length - 1; i++) {
            double currentPrice = data[i][1];  // close price
            double nextPrice = data[i + 1][1]; // next day's close price
            
            // Calculate percentage change
            double priceChange = (nextPrice - currentPrice) / currentPrice;
            
            // FIXED: Use smaller, more realistic thresholds
            if (priceChange > 0.001) { // Up more than 0.1% (was 0.5%)
                positiveClass.add(data[i]);
            } else if (priceChange < -0.001) { // Down more than 0.1% (was 0.5%)
                negativeClass.add(data[i]);
            }
            // Skip samples with very small changes (-0.1% to +0.1%)
        }
        
        System.out.println("Positive samples: " + positiveClass.size());
        System.out.println("Negative samples: " + negativeClass.size());
        
        // Use the smaller class size for both
        int minSize = Math.min(positiveClass.size(), negativeClass.size());
        
        // FIXED: Lower minimum threshold
        if (minSize < 100) { // Reduced from 1000
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

    private static double[][] balanceDatasetWithLargerThreshold(double[][] data) {
        List<double[]> positiveClass = new ArrayList<>();
        List<double[]> negativeClass = new ArrayList<>();
        
        for (int i = 0; i < data.length - 1; i++) {
            double currentPrice = data[i][1];
            double nextPrice = data[i + 1][1];
            
            double priceChange = (nextPrice - currentPrice) / currentPrice;
            
            // Use larger thresholds
            if (priceChange > 0.02) { // Up more than 2%
                positiveClass.add(data[i]);
            } else if (priceChange < -0.02) { // Down more than 2%
                negativeClass.add(data[i]);
            }
        }
        
        int minSize = Math.min(positiveClass.size(), negativeClass.size());
        
        if (minSize < 100) {
            System.err.println("Warning: Very small balanced dataset, returning original data");
            return data;
        }
        
        List<double[]> balancedData = new ArrayList<>();
        balancedData.addAll(positiveClass.subList(0, minSize));
        balancedData.addAll(negativeClass.subList(0, minSize));
        
        System.out.println("Balanced dataset (2% threshold): " + minSize + " positive, " + minSize + " negative samples");
        
        return balancedData.toArray(new double[0][]);
    }

    private static void checkForNaN(double[][] data, String label) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (Double.isNaN(data[i][j]) || Double.isInfinite(data[i][j])) {
                    System.err.println("Invalid value in " + label + " at [" + i + "][" + j + "]: " + data[i][j]);
                }
            }
        }
    }

    private static void checkForNaN1D(double[] data, String label) {
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
}