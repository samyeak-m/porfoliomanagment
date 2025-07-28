package com.stockmanagment.porfoliomanagment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lstm")
public class LstmConfig {
    private String version;
    private int hiddenSize;
    private int denseSize;
    private int inputSize;
    private int outputSize;
    private int epochs;
    private int batchSize;
    private double trainingRate;
    private double threshold;
    private int retrainThresholdDays;
    private String baseDir;

    // Color constants
    public String getResetColor() { return "\u001B[0m"; }
    public String getGreenColor() { return "\u001B[32m"; }
    public String getBlueColor() { return "\u001B[34m"; }
    public String getYellowColor() { return "\u001B[33m"; }
    
    // Computed properties
    public String getOutputDir() {
        return baseDir + "/output_" + version + "_e" + epochs + "_b" + batchSize + "_h" + hiddenSize;
    }

    public String getModelFilePath() {
        return getOutputDir() + "/lstm_model" + version + "_" + epochs + ".ser";
    }

    public String getLastTrainingDateFile() {
        return getOutputDir() + "/last_training_date.txt";
    }

    // Compatibility aliases
    public int getBatch() { return batchSize; }
    public int getInterval() { return 100; }

    // Standard getters and setters (NO @Value annotations)
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public int getHiddenSize() { return hiddenSize; }
    public void setHiddenSize(int hiddenSize) { this.hiddenSize = hiddenSize; }

    public int getDenseSize() { return denseSize; }
    public void setDenseSize(int denseSize) { this.denseSize = denseSize; }

    public int getInputSize() { return inputSize; }
    public void setInputSize(int inputSize) { this.inputSize = inputSize; }

    public int getOutputSize() { return outputSize; }
    public void setOutputSize(int outputSize) { this.outputSize = outputSize; }

    public int getEpochs() { return epochs; }
    public void setEpochs(int epochs) { this.epochs = epochs; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public double getTrainingRate() { return trainingRate; }
    public void setTrainingRate(double trainingRate) { this.trainingRate = trainingRate; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    public int getRetrainThresholdDays() { return retrainThresholdDays; }
    public void setRetrainThresholdDays(int retrainThresholdDays) { this.retrainThresholdDays = retrainThresholdDays; }

    public String getBaseDir() { return baseDir; }
    public void setBaseDir(String baseDir) { this.baseDir = baseDir; }
}