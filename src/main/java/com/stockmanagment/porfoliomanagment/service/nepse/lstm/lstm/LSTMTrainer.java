package com.stockmanagment.porfoliomanagment.service.nepse.lstm.lstm;

import java.util.Arrays;
import java.util.Random;

public class LSTMTrainer {
    private final LSTMNetwork lstm;
    private final double learningRate;

    public LSTMTrainer(LSTMNetwork lstm, double learningRate) {
        this.lstm = lstm;
        this.learningRate = learningRate;
    }

    public void train(double[][] data, int epochs, int batchSize) {
        Random rng = new Random(42);
        for (int epoch = 0; epoch < epochs; epoch++) {
            trainEpoch(data, batchSize, learningRate, rng);
        }
    }

    // Use next-step target (data[i+1][1]) and keep full input width
    public void trainEpoch(double[][] data, int batchSize, double lr, Random rng) {
        int n = data.length;
        if (n < 2) return;

        int[] idx = new int[n - 1]; // last index can't be a source (no next target)
        for (int i = 0; i < idx.length; i++) idx[i] = i;

        // Fisher–Yates shuffle
        for (int i = idx.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = idx[i]; idx[i] = idx[j]; idx[j] = tmp;
        }

        for (int start = 0; start < idx.length; start += batchSize) {
            int end = Math.min(start + batchSize, idx.length);
            for (int k = start; k < end; k++) {
                int i = idx[k];
                double[] input = Arrays.copyOf(data[i], data[i].length); // full feature vector
                double target = data[i + 1][1]; // next-step close (normalized)
                lstm.backpropagate(input, new double[]{target}, lr);
            }
        }
    }

    private void trainBatch(double[][] batch) {
        for (int i = 0; i < batch.length - 1; i++) {
            double[] input = Arrays.copyOf(batch[i], batch[i].length);
            double target = batch[i + 1][1];
            lstm.backpropagate(input, new double[]{target}, learningRate);
        }
    }

    private double[][] getBatch(double[][] data, int start, int batchSize) {
        int end = Math.min(start + batchSize, data.length);
        double[][] batch = new double[end - start][];
        System.arraycopy(data, start, batch, 0, batch.length);
        return batch;
    }
}
