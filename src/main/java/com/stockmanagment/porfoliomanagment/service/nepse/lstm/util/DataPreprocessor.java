package com.stockmanagment.porfoliomanagment.service.nepse.lstm.util;

import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
public class DataPreprocessor {

    private static final String NORMALIZATION_FILE_PATH = "normal.txt";


    // Normalize data using min-max scaling to range [0, 1]
    public static double[][] normalize(double[][] data, double[] min, double[] max) {
        double[][] normalizedData = new double[data.length][data[0].length];
        try (PrintWriter writer = new PrintWriter(new FileWriter(NORMALIZATION_FILE_PATH, true))) {

            for (int i = 0; i < data.length; i++) {
                // Skip normalization for the first feature
                normalizedData[i][0] = data[i][0];

                for (int j = 1; j < data[i].length; j++) {  // Start from index 1
                    double range = max[j] - min[j];
                    if (range == 0) {
                        // Avoid division by zero when min equals max
                        normalizedData[i][j] = 0.5;  // Set to midpoint if no range
                    } else {
                        normalizedData[i][j] = (data[i][j] - min[j]) / range;
                    }
                    // Ensure normalized value is within [0, 1] range
                    normalizedData[i][j] = Math.max(0, Math.min(1, normalizedData[i][j]));
//                    writer.println("Feature " + j + ": Original value: " + data[i][j] +
//                            ", Min value: " + min[j] + ", Max value: " + max[j] +
//                            ", Normalized value: " + normalizedData[i][j]+",");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return normalizedData;
    }


    // Add additional features (technical indicators) to the dataset
    public static double[][] addFeatures(double[][] data, double[][] technicalIndicators) {
        double[][] extendedData = new double[data.length][data[0].length + technicalIndicators[0].length];
        for (int i = 0; i < data.length; i++) {
            System.arraycopy(data[i], 0, extendedData[i], 0, data[i].length);
            System.arraycopy(technicalIndicators[i], 0, extendedData[i], data[i].length, technicalIndicators[i].length);
        }
        return extendedData;
    }

    // Calculate minimum value for each feature in the dataset
    public static double[] calculateMin(double[][] data) {
        double[] min = Arrays.copyOf(data[0], data[0].length);
        for (double[] row : data) {
            for (int i = 0; i < row.length; i++) {
                if (row[i] < min[i]) {
                    min[i] = row[i];
                }
            }
        }
        return min;
    }

    // Calculate maximum value for each feature in the dataset
    public static double[] calculateMax(double[][] data) {
        double[] max = Arrays.copyOf(data[0], data[0].length);
        for (double[] row : data) {
            for (int i = 0; i < row.length; i++) {
                if (row[i] > max[i]) {
                    max[i] = row[i];
                }
            }
        }
        return max;
    }

    // Denormalize data from [0, 1] range back to original scale
    public static double[] denormalize(double[] normalizedValues, double min, double max) {
        double[] denormalizedValues = new double[normalizedValues.length];
        for (int i = 0; i < normalizedValues.length; i++) {
            denormalizedValues[i] = normalizedValues[i] * (max - min) + min;
        }
        return denormalizedValues;
    }
}
