package com.stockmanagment.porfoliomanagment.lstm;

import com.stockmanagment.porfoliomanagment.lstm.database.DatabaseHelper;
import com.stockmanagment.porfoliomanagment.lstm.lstm.LSTMNetwork;
import com.stockmanagment.porfoliomanagment.lstm.lstm.LSTMTrainer;
import com.stockmanagment.porfoliomanagment.lstm.util.CustomChartUtils;
import com.stockmanagment.porfoliomanagment.lstm.util.DataPreprocessor;
import com.stockmanagment.porfoliomanagment.lstm.util.TechnicalIndicators;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    static String version = "v1";

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";
    private static final String YELLOW = "\u001B[33m";

    static int hiddenSize = 50;
    static int denseSize = 5;
    static int inputSize = 8;
    static int outputSize = 1;
    static int epoch = 1000;
    static int batch = 64;
    static double trainingRate = 0.1;

    static double threshold = 0.1;
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

            double[][] technicalIndicators = TechnicalIndicators.calculate(stockDataArray, 16, 3);

            double[][] extendedData = DataPreprocessor.addFeatures(stockDataArray, technicalIndicators);

            double[][][] preprocessedData = preprocessData(extendedData, 0.6);
            double[][] trainData = preprocessedData[0];
            double[][] testData = preprocessedData[1];

            double[][] validationData = Arrays.copyOfRange(testData, 0, testData.length / 5);
            double[][] finalTestData = Arrays.copyOfRange(testData, testData.length / 5, testData.length);

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
            double[] metrics = printConfusionMatrix(confusionMatrix);

            double averageAccuracy = averages[0];
            double averageLoss = averages[1];
            double precision = metrics[0];
            double recall = metrics[1];
            double f1Score = metrics[2];

            logFile(testAccuracy, finalTestLoss, averageAccuracy, averageLoss, confusionMatrix, precision, recall, f1Score, trainData, validationData, finalTestData);

            lstm.saveModel(MODEL_FILE_PATH);

            String accuracyChartDir = BASE_DIR + File.separator + "charts" + version + "_" + epoch + File.separator + "accuracy";
            createDirectory(accuracyChartDir);

            CustomChartUtils.saveAccuracyChart("Model Accuracy", epochList, accuracyList, validationAccuracyList, accuracyChartDir + File.separator + "model_accuracy.png", "Epochs", "Accuracy",interval);
            CustomChartUtils.saveLossChart("Model Loss", epochList, lossList, validationLossList, accuracyChartDir + File.separator + "model_loss.png", "Epochs", "Loss",interval);

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

    private static double[] printConfusionMatrix(int[][] matrix) {

        int tp = matrix[0][0];
        int fn = matrix[1][0];
        int fp = matrix[0][1];
        int tn = matrix[1][1];

        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0;
        double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0;
        double f1Score = (precision + recall) > 0 ? 2 * (precision * recall) / (precision + recall) : 0;

        System.out.println("Confusion Matrix:");
        System.out.println("TP: " + tp + ", FN: " + fn);
        System.out.println("FP: " + fp + ", TN: " + tn);
        System.out.println("Precision: " + String.format("%.4f", precision));
        System.out.println("Recall: " + String.format("%.4f", recall));
        System.out.println("F1 Score: " + String.format("%.4f", f1Score));

        return new double[]{precision, recall, f1Score};

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
                    double[] input = Arrays.copyOfRange(data, 0, data.length);
                    double[] target = new double[]{data[data.length - 1]};
                    lstm.backpropagate(input, target, learningRate);

                    double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());

                    if (output == null) {
                        LOGGER.severe("NaN value encountered during forward pass. Stopping training.");
                        return new double[]{0, 0};
                    }
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
            double[] input = Arrays.copyOf(testData[i], testData[i].length - 1);
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            if (output == null) {
                continue;
            }
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

    private static double applyPredictionConstraints(double prediction, double lastClosePrice) {
        double maxChange = 0.08 * lastClosePrice;
        double minPrice = lastClosePrice * 0.92;
        double maxPrice = lastClosePrice * 1.08;

        if (prediction < minPrice) {
            prediction = minPrice;
        } else if (prediction > maxPrice) {
            prediction = maxPrice;
        }

        prediction = Math.max(0, Math.min(1, prediction));

        return prediction;
    }


    private static double calculatePredictionAccuracy(double prediction, double actual, double currentClosePrice) {
        double maxChange = 0.08 * currentClosePrice;
        double diff = Math.abs(prediction - actual);;

        if (diff > maxChange) {
            return 0;
        }

        double accuracy = 1 - (diff / maxChange);
        return accuracy;
    }



    private static double calculateLoss(LSTMNetwork lstm, double[][] data) {
        double totalLoss = 0;
        double maxChange = 0.08;

        for (int i = 0; i < data.length - 1; i++) {
            double[] input = Arrays.copyOf(data[i], data[i].length - 1);
            double lastClosePrice = data[i][1];
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            double prediction = output[0];

            double minPrice = lastClosePrice * (1 - maxChange);
            double maxPrice = lastClosePrice * (1 + maxChange);

            if (prediction < minPrice) {
                prediction = minPrice;
            } else if (prediction > maxPrice) {
                prediction = maxPrice;
            }

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
                               double precision, double recall, double f1Score, double[][] trainData, double[][] validationData, double[][] finalTestData) {
        String logFileName = BASE_DIR + File.separator + "confusion.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName))) {

            writer.write("hiddenSize: " + hiddenSize + ", epoch: " + epoch + ", batch: " + batch + ", trainingRate: " + trainingRate + "\n");
            writer.write("Training data size: " + trainData.length + "\n");
            writer.write("Validation data size: " + validationData.length + "\n");
            writer.write("Final test data size: " + finalTestData.length + "\n");

            writer.write("Final Test Accuracy: " + String.format("%.2f", finalTestAccuracy) + "\n");
            writer.write("Final Test Loss: " + String.format("%.2f", finalTestLoss) + "\n");
            writer.write("Overall Average Accuracy: " + String.format("%.2f", averageAccuracy) + "\n");
            writer.write("Overall Average Loss: " + String.format("%.2f", averageLoss) + "\n");

            int tp = confusionMatrix[0][0];
            int fn = confusionMatrix[1][0];
            int fp = confusionMatrix[0][1];
            int tn = confusionMatrix[1][1];

            writer.write("Confusion Matrix:\n");
            writer.write("TP: " + tp + ", FN: " + fn + "\n");
            writer.write("FP: " + fp + ", TN: " + tn + "\n");
            writer.write("Precision: " + String.format("%.4f", precision) + "\n");
            writer.write("Recall: " + String.format("%.4f", recall) + "\n");
            writer.write("F1 Score: " + String.format("%.4f", f1Score) + "\n");

            for (int i = 0; i < epochList.size(); i++) {
                writer.write(String.format("Epoch %d: Accuracy = %.2f, Loss = %.2f, Validation Accuracy = %.2f, Validation Loss = %.2f\n",
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

        double[][] technicalIndicators = TechnicalIndicators.calculate(stockDataArray,  16, 3);

        double[][] extendedData = DataPreprocessor.addFeatures(stockDataArray, technicalIndicators);

        extendedData = DataPreprocessor.normalize(extendedData, min, max);

        int days = 1;

        double[] predictions = new double[days];
        for (int i = 0; i < days; i++) {
            double[] input = Arrays.copyOfRange(extendedData[extendedData.length - 1], 0, extendedData[0].length);
            double[] output = lstm.forward(input, lstm.getHiddenState(), lstm.getCellState());
            double prediction = output[0];

            double lastClosePrice = extendedData[extendedData.length - 1][1];

            prediction = applyPredictionConstraints(prediction, lastClosePrice);

            predictions[i] = prediction;

            double[] newInput = new double[extendedData[0].length];
            System.arraycopy(input, 1, newInput, 0, input.length - 1);
            newInput[newInput.length - 1] = predictions[i];
            extendedData = Arrays.copyOf(extendedData, extendedData.length + 1);
            extendedData[extendedData.length - 1] = newInput;
        }

        predictions = DataPreprocessor.denormalize(predictions, min[min.length - 1], max[max.length - 1]);

        double[] actualPrices = new double[days];
        for (int i = 0; i < days; i++) {
            actualPrices[i] = stockDataArray[stockDataArray.length - 1][1];
        }

        dbHelper.savePredictions(stockSymbol, predictions, actualPrices);
        System.out.println(GREEN + "Predictions saved for stock: " + stockSymbol + RESET);
    }

    private static double[][][] preprocessData(double[][] data, double trainSplitRatio) {
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

            if (bufferedMin < 0) {
                bufferedMin = 0;
            }

            min[i] = bufferedMin;
            max[i] = bufferedMax;
        }


        trainData = DataPreprocessor.normalize(trainData, min, max);
        testData = DataPreprocessor.normalize(testData, min, max);

        return new double[][][]{trainData, testData};
    }


}
