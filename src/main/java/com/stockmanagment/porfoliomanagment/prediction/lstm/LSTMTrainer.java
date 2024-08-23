package com.stockmanagment.porfoliomanagment.prediction.lstm;

public class LSTMTrainer {
    private final LSTMNetwork lstm;
    private final double learningRate;

    public LSTMTrainer(LSTMNetwork lstm, double learningRate) {
        this.lstm = lstm;
        this.learningRate = learningRate;
    }

    public void train(double[][] data, int epochs, int batchSize) {
        for (int epoch = 0; epoch < epochs; epoch++) {
            for (int i = 0; i < data.length; i += batchSize) {
                double[][] batch = getBatch(data, i, batchSize);
                trainBatch(batch);
            }
        }
    }

    private void trainBatch(double[][] batch) {
        for (double[] sample : batch) {
            double[] input = new double[sample.length - 1];
            System.arraycopy(sample, 0, input, 0, input.length);
            double[] target = new double[]{sample[sample.length - 1]};

            lstm.backpropagate(input, target, learningRate);
        }
    }

    private double[][] getBatch(double[][] data, int start, int batchSize) {
        int end = Math.min(start + batchSize, data.length);
        double[][] batch = new double[end - start][];
        System.arraycopy(data, start, batch, 0, batch.length);
        return batch;
    }
}
