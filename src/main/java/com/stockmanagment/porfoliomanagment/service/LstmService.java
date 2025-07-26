package com.stockmanagment.porfoliomanagment.service;

import com.stockmanagment.porfoliomanagment.dto.PredictionRequestDTO;
import com.stockmanagment.porfoliomanagment.dto.PredictionResponseDTO;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.database.DatabaseHelper;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.lstm.LSTMNetwork;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.lstm.LSTMTrainer;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.DataPreprocessor;
import com.stockmanagment.porfoliomanagment.service.nepse.lstm.util.TechnicalIndicators;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@Service
public class LstmService {

    private static final String VERSION = "v1";
    private static final int HIDDEN_SIZE = 20;
    private static final int DENSE_SIZE = 3;
    private static final int INPUT_SIZE = 8;
    private static final int OUTPUT_SIZE = 1;
    private static final int EPOCH = 10;
    private static final int BATCH = 16;
    private static final double TRAINING_RATE = 0.1;
    private static final String BASE_DIR = "output_" + VERSION + "_e" + EPOCH + "_b" + BATCH + "_h" + HIDDEN_SIZE;
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
}
