package com.stockmanagment.porfoliomanagment.service.nepse.lstm.util;

import org.springframework.stereotype.Service;

public class TechnicalIndicators {

    public static double[] calculateSMA(double[] prices, int period) {
        double[] sma = new double[prices.length];
        for (int i = 0; i < prices.length; i++) {
            if (i < period - 1) {
                sma[i] = 0;
            } else {
                double sum = 0;
                for (int j = 0; j < period; j++) {
                    sum += prices[i - j];
                }
                sma[i] = sum / period;
            }
        }
        return sma;
    }

    public static double[] calculateEMA(double[] prices, int period) {
        double[] ema = new double[prices.length];
        double multiplier = 2.0 / (period + 1);
        ema[0] = prices[0];
        for (int i = 1; i < prices.length; i++) {
            ema[i] = ((prices[i] - ema[i - 1]) * multiplier) + ema[i - 1];
        }
        return ema;
    }

    public static double[] calculateRSI(double[] prices, int period) {
        double[] rsi = new double[prices.length];
        double[] gains = new double[prices.length];
        double[] losses = new double[prices.length];

        for (int i = 1; i < prices.length; i++) {
            double change = prices[i] - prices[i - 1];
            if (change > 0) {
                gains[i] = change;
                losses[i] = 0;
            } else {
                gains[i] = 0;
                losses[i] = -change;
            }
        }

        double averageGain = 0;
        double averageLoss = 0;
        for (int i = 1; i <= period; i++) {
            averageGain += gains[i];
            averageLoss += losses[i];
        }
        averageGain /= period;
        averageLoss /= period;

        for (int i = period; i < prices.length; i++) {
            if (i > period) {
                averageGain = ((averageGain * (period - 1)) + gains[i]) / period;
                averageLoss = ((averageLoss * (period - 1)) + losses[i]) / period;
            }

            double rs = averageGain / averageLoss;
            rsi[i] = 100 - (100 / (1 + rs));
        }
        return rsi;
    }

    public static double[][] calculateMACD(double[] prices, int shortPeriod, int longPeriod, int signalPeriod) {
        double[] emaShort = calculateEMA(prices, shortPeriod);
        double[] emaLong = calculateEMA(prices, longPeriod);
        double[] macd = new double[prices.length];
        for (int i = 0; i < prices.length; i++) {
            macd[i] = emaShort[i] - emaLong[i];
        }
        double[] signal = calculateEMA(macd, signalPeriod);
        double[] histogram = new double[prices.length];
        for (int i = 0; i < prices.length; i++) {
            histogram[i] = macd[i] - signal[i];
        }
        return new double[][]{macd, signal, histogram};
    }

    public static double[][] calculateBollingerBands(double[] prices, int period, double stdDevMultiplier) {
        double[] sma = calculateSMA(prices, period);
        double[] upperBand = new double[prices.length];
        double[] lowerBand = new double[prices.length];

        for (int i = period - 1; i < prices.length; i++) {
            double sum = 0;
            for (int j = 0; j < period; j++) {
                sum += Math.pow(prices[i - j] - sma[i], 2);
            }
            double stdDev = Math.sqrt(sum / period);
            upperBand[i] = sma[i] + (stdDevMultiplier * stdDev);
            lowerBand[i] = sma[i] - (stdDevMultiplier * stdDev);
        }
        return new double[][]{sma, upperBand, lowerBand};
    }

    public static double[] calculateATR(double[] high, double[] low, double[] close, int period) {
        double[] atr = new double[close.length];
        double[] tr = new double[close.length];

        for (int i = 1; i < close.length; i++) {
            double highLow = high[i] - low[i];
            double highClose = Math.abs(high[i] - close[i - 1]);
            double lowClose = Math.abs(low[i] - close[i - 1]);
            tr[i] = Math.max(highLow, Math.max(highClose, lowClose));
        }

        double sum = 0;
        for (int i = 1; i <= period; i++) {
            sum += tr[i];
        }
        atr[period] = sum / period;

        for (int i = period + 1; i < close.length; i++) {
            atr[i] = ((atr[i - 1] * (period - 1)) + tr[i]) / period;
        }
        return atr;
    }

    public static double[][] calculateStochasticOscillator(double[] close, double[] high, double[] low, int period) {
        double[] k = new double[close.length];
        double[] d = new double[close.length];

        for (int i = period - 1; i < close.length; i++) {
            double highestHigh = Double.MIN_VALUE;
            double lowestLow = Double.MAX_VALUE;
            for (int j = 0; j < period; j++) {
                if (high[i - j] > highestHigh) {
                    highestHigh = high[i - j];
                }
                if (low[i - j] < lowestLow) {
                    lowestLow = low[i - j];
                }
            }
            k[i] = ((close[i] - lowestLow) / (highestHigh - lowestLow)) * 100;
        }

        for (int i = period * 2 - 2; i < close.length; i++) {
            double sum = 0;
            for (int j = 0; j < period; j++) {
                sum += k[i - j];
            }
            d[i] = sum / period;
        }
        return new double[][]{k, d};
    }

    public static double[][] calculate(double[][] stockData,int emaPeriod, int rsiPeriod) {
        int priceIndex = 1;

        double[] prices = new double[stockData.length];

        for (int i = 0; i < stockData.length; i++) {
            prices[i] = stockData[i][priceIndex];

        }

//        double[] sma = calculateSMA(prices, smaPeriod);
        double[] ema = calculateEMA(prices, emaPeriod);
        double[] rsi = calculateEMA(prices, rsiPeriod);

        double[][] indicators = new double[stockData.length][2];

        for (int i = 0; i < stockData.length; i++) {
//            indicators[i][0] = sma[i];
            indicators[i][0] = ema[i];
            indicators[i][1] = rsi[i];
        }

        return indicators;
    }
}
