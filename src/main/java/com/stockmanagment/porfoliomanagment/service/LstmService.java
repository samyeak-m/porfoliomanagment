package com.stockmanagment.porfoliomanagment.service;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.stockmanagment.porfoliomanagment.dto.PredictionResponseDTO;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.database.DatabaseHelper;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.lstm.LSTMNetwork;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.lstm.LSTMTrainer;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.CustomChartUtils;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.DataPreprocessor;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.TechnicalIndicators;

@Service
public class LstmService {

    private static final String VERSION = "v2";
    private static final int HIDDEN_SIZE = 20;
    private static final int DENSE_SIZE = 3;
    private static final int INPUT_SIZE = 8;
    private static final int OUTPUT_SIZE = 1;
    private static final int EPOCH = 10;
    private static final int BATCH = 16;
    private static final double TRAINING_RATE = 0.1;
    private static final String BASE_DIR = "src/main/resources/static/model/output_" + VERSION + "_e" + EPOCH + "_b" + BATCH + "_h" + HIDDEN_SIZE;
    private static final String MODEL_FILE_PATH = BASE_DIR + File.separator + "lstm_model" + VERSION + "_" + EPOCH + ".ser";

    private double[] min;
    private double[] max;

    public void train() {
        try {
            DatabaseHelper dbHelper = new DatabaseHelper();
            List<String> tableNames = dbHelper.getAllStockTableNames();
            List<double[]> allStockData = new ArrayList<>();
            for (String tableName : tableNames) {
                allStockData.addAll(dbHelper.loadStockData(tableName));
            }
            double[][] stockDataArray = allStockData.toArray(new double[0][]);
            double[][] technicalIndicators = TechnicalIndicators.calculate(stockDataArray, 16, 3);
            double[][] extendedData = DataPreprocessor.addFeatures(stockDataArray, technicalIndicators);

            double[][][] preprocessedData = preprocessData(extendedData, 0.6);
            double[][] trainData = preprocessedData[0];
            double[][] testData = preprocessedData[1];

            double[][] validationData = Arrays.copyOfRange(testData, 0, testData.length / 5);

            min = DataPreprocessor.calculateMin(extendedData);
            max = DataPreprocessor.calculateMax(extendedData);

            LSTMNetwork lstm = new LSTMNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, DENSE_SIZE, min, max);
            LSTMTrainer trainer = new LSTMTrainer(lstm, TRAINING_RATE);
            trainer.train(trainData, EPOCH, BATCH);

            // Evaluate the model on test data and assign metrics
            double testAccuracy = testModel(lstm, testData);
            double testLoss = calculateLoss(lstm, testData);
            int[][] confusionMatrix = lstm.computeConfusionMatrix(testData, testData[testData.length - 1][1], 0.1);
            double f1Score = computeF1Score(confusionMatrix);

            // 2. Save metrics to confusion.txt
            File metricsFile = new File(BASE_DIR + File.separator + "confusion.txt");
            try (PrintWriter writer = new PrintWriter(metricsFile)) {
                writer.println("Test Accuracy: " + testAccuracy);
                writer.println("Test Loss: " + testLoss);
                writer.println("F1 Score: " + f1Score);
                writer.println("Confusion Matrix:");
                // Write confusion matrix rows
                for (int[] row : confusionMatrix) {
                    writer.println(Arrays.toString(row));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            List<Integer> epochs = new ArrayList<>();
            List<Double> accuracyList = new ArrayList<>();
            List<Double> lossList = new ArrayList<>(); 
            List<Double> valAccuracyList = new ArrayList<>();
            List<Double> valLossList = new ArrayList<>();

            String chartDir = BASE_DIR + File.separator + "charts";
            new File(chartDir).mkdirs();

            CustomChartUtils.saveAccuracyChart(
                "Model Accuracy", epochs, accuracyList, valAccuracyList,
                chartDir + File.separator + "model_accuracy.png", "Epochs", "Accuracy", 1);

            CustomChartUtils.saveLossChart(
                "Model Loss", epochs, lossList, valLossList,
                chartDir + File.separator + "model_loss.png", "Epochs", "Loss", 1);

            File modelDir = new File(BASE_DIR);
            if (!modelDir.exists()) {
                modelDir.mkdirs();
            }

            lstm.saveModel(MODEL_FILE_PATH);
        } catch (Exception e) {
            throw new RuntimeException("Training failed: " + e.getMessage(), e);
        }
    }

    public PredictionResponseDTO predict(String stockSymbol) {
        try {
            DatabaseHelper dbHelper = new DatabaseHelper();
            LSTMNetwork lstm = LSTMNetwork.loadModel(MODEL_FILE_PATH);
            if (lstm == null) throw new RuntimeException("Model not trained yet.");
            min = lstm.getMin();
            max = lstm.getMax();

            List<double[]> stockData = dbHelper.loadStockData(stockSymbol);
            double[][] stockDataArray = stockData.toArray(new double[0][]);
            double[][] technicalIndicators = TechnicalIndicators.calculate(stockDataArray, 16, 3);
            double[][] extendedData = DataPreprocessor.addFeatures(stockDataArray, technicalIndicators);
            extendedData = DataPreprocessor.normalize(extendedData, min, max);

            double[] input = Arrays.copyOfRange(extendedData[extendedData.length - 1], 0, extendedData[0].length);
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            double prediction = output[0];
            double lastClosePrice = extendedData[extendedData.length - 1][1];

            prediction = applyPredictionConstraints(prediction, lastClosePrice);

            double[] denormPrediction = DataPreprocessor.denormalize(new double[]{prediction}, min[min.length - 1], max[max.length - 1]);
            double[] denormLastClose = DataPreprocessor.denormalize(new double[]{lastClosePrice}, min[min.length - 1], max[max.length - 1]);
            double pointChange = denormPrediction[0] - denormLastClose[0];
            double priceChange = (pointChange / denormLastClose[0]) * 100;

            PredictionResponseDTO response = new PredictionResponseDTO();
            response.setStockSymbol(stockSymbol);
            response.setPrediction(denormPrediction[0]);
            response.setLastClose(denormLastClose[0]);
            response.setPointChange(pointChange);
            response.setPriceChange(priceChange);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }

    private double applyPredictionConstraints(double prediction, double lastClosePrice) {
        double maxChange = 0.08 * lastClosePrice;
        double minPrice = lastClosePrice * 0.92;
        double maxPrice = lastClosePrice * 1.08;
        if (prediction < minPrice) prediction = minPrice;
        else if (prediction > maxPrice) prediction = maxPrice;
        prediction = Math.max(0, Math.min(1, prediction));
        return prediction;
    }

    private double[][][] preprocessData(double[][] data, double trainSplitRatio) {
        int trainSize = (int) (data.length * trainSplitRatio);
        double[][] trainData = Arrays.copyOfRange(data, 0, trainSize);
        double[][] testData = Arrays.copyOfRange(data, trainSize, data.length);

        min = DataPreprocessor.calculateMin(data);
        max = DataPreprocessor.calculateMax(data);

        double bufferPercentage = 0.40;
        for (int i = 1; i < min.length; i++) {
            double actualMin = min[i];
            double actualMax = max[i];
            double bufferedMin = actualMin - (bufferPercentage * (actualMax - actualMin));
            double bufferedMax = actualMax + (bufferPercentage * (actualMax - actualMin));
            if (bufferedMin < 0) bufferedMin = 0;
            min[i] = bufferedMin;
            max[i] = bufferedMax;
        }
        trainData = DataPreprocessor.normalize(trainData, min, max);
        testData = DataPreprocessor.normalize(testData, min, max);

        return new double[][][]{trainData, testData};
    }

    private double testModel(LSTMNetwork lstm, double[][] testData) {
        double totalAccuracy = 0;
        for (int i = 0; i < testData.length - 1; i++) {
            double[] input = Arrays.copyOf(testData[i], testData[i].length - 1);
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            if (output == null) continue;
            double prediction = output[0];
            double actual = testData[i + 1][1];
            double currentClosePrice = testData[i][1];
            double lastClosePrice = testData[i][1];
            prediction = applyPredictionConstraints(prediction, lastClosePrice);
            double accuracy = calculatePredictionAccuracy(prediction, actual, currentClosePrice);
            totalAccuracy += accuracy;
        }
        return totalAccuracy / (testData.length - 1);
    }

    private double calculateLoss(LSTMNetwork lstm, double[][] data) {
        double totalLoss = 0;
        double maxChange = 0.08;
        for (int i = 0; i < data.length - 1; i++) {
            double[] input = Arrays.copyOf(data[i], data[i].length - 1);
            double lastClosePrice = data[i][1];
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            double prediction = output[0];
            double minPrice = lastClosePrice * (1 - maxChange);
            double maxPrice = lastClosePrice * (1 + maxChange);
            if (prediction < minPrice) prediction = minPrice;
            else if (prediction > maxPrice) prediction = maxPrice;
            double actual = data[i + 1][1];
            double diff = Math.abs(prediction - actual);
            double tolerance = maxChange * actual;
            double loss = (diff > tolerance) ? 1.0 : diff / tolerance;
            totalLoss += loss;
        }
        return totalLoss / (data.length - 1);
    }

    private double calculatePredictionAccuracy(double prediction, double actual, double currentClosePrice) {
        double maxChange = 0.08 * currentClosePrice;
        double diff = Math.abs(prediction - actual);
        if (diff > maxChange) return 0;
        return 1 - (diff / maxChange);
    }

    private double computeF1Score(int[][] confusionMatrix) {
        int tp = confusionMatrix[0][0];
        int fn = confusionMatrix[1][0];
        int fp = confusionMatrix[0][1];
        int tn = confusionMatrix[1][1];
        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0;
        double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0;
        return (precision + recall) > 0 ? 2 * (precision * recall) / (precision + recall) : 0;
    }
}
