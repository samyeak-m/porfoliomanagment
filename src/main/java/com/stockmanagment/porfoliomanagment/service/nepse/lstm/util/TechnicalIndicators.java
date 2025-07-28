package com.stockmanagment.porfoliomanagment.service.nepse.lstm.util;

public class TechnicalIndicators {

    public static double[] calculateSMA(double[] prices, int period) {
        double[] sma = new double[prices.length];
        for (int i = 0; i < prices.length; i++) {
            if (i < period - 1) {
                // For early values, use average of available data
                double sum = 0;
                for (int j = 0; j <= i; j++) {
                    sum += prices[j];
                }
                sma[i] = sum / (i + 1);
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

        // FIXED: Proper EMA initialization
        ema[0] = prices[0]; // Start with first price

        for (int i = 1; i < prices.length; i++) {
            if (i < period) {
                // For early values, use SMA of available data
                double sum = 0;
                for (int j = 0; j <= i; j++) {
                    sum += prices[j];
                }
                ema[i] = sum / (i + 1);
            } else {
                // Standard EMA calculation
                ema[i] = ((prices[i] - ema[i - 1]) * multiplier) + ema[i - 1];
            }
        }
        return ema;
    }

    public static double[] calculateRSI(double[] prices, int period) {
        double[] rsi = new double[prices.length];
        double[] gains = new double[prices.length];
        double[] losses = new double[prices.length];

        // Calculate gains and losses
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

        // Calculate initial averages
        double averageGain = 0;
        double averageLoss = 0;
        for (int i = 1; i <= period && i < prices.length; i++) {
            averageGain += gains[i];
            averageLoss += losses[i];
        }
        averageGain /= period;
        averageLoss /= period;

        // Fill early RSI values with 50 (neutral)
        for (int i = 0; i < period && i < prices.length; i++) {
            rsi[i] = 50.0;
        }

        // Calculate RSI for remaining values
        for (int i = period; i < prices.length; i++) {
            if (i > period) {
                averageGain = ((averageGain * (period - 1)) + gains[i]) / period;
                averageLoss = ((averageLoss * (period - 1)) + losses[i]) / period;
            }

            // Prevent division by zero and handle edge cases
            if (averageLoss == 0) {
                rsi[i] = 100; // RSI = 100 when there are no losses
            } else if (averageGain == 0) {
                rsi[i] = 0;   // RSI = 0 when there are no gains
            } else {
                double rs = averageGain / averageLoss;
                rsi[i] = 100 - (100 / (1 + rs));
            }
        }
        return rsi;
    }

    public static double[][] calculateMACD(double[] prices, int shortPeriod, int longPeriod, int signalPeriod) {
        double[] emaShort = calculateEMA(prices, shortPeriod);
        double[] emaLong = calculateEMA(prices, longPeriod);
        double[] macd = new double[prices.length];
        
        // Calculate MACD line
        for (int i = 0; i < prices.length; i++) {
            macd[i] = emaShort[i] - emaLong[i];
        }
        
        // Calculate signal line (EMA of MACD)
        double[] signal = calculateEMA(macd, signalPeriod);
        
        // Calculate histogram
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

        // FIXED: Handle early values
        for (int i = 0; i < prices.length; i++) {
            if (i < period - 1) {
                // For early values, use price ± small percentage
                upperBand[i] = prices[i] * 1.02; // 2% above
                lowerBand[i] = prices[i] * 0.98; // 2% below
            } else {
                double sum = 0;
                int actualPeriod = Math.min(period, i + 1);
                for (int j = 0; j < actualPeriod; j++) {
                    sum += Math.pow(prices[i - j] - sma[i], 2);
                }
                double stdDev = Math.sqrt(sum / actualPeriod);
                upperBand[i] = sma[i] + (stdDevMultiplier * stdDev);
                lowerBand[i] = sma[i] - (stdDevMultiplier * stdDev);
            }
        }
        return new double[][]{sma, upperBand, lowerBand};
    }

    public static double[] calculateATR(double[] high, double[] low, double[] close, int period) {
        double[] atr = new double[close.length];
        double[] tr = new double[close.length];

        // First TR value
        tr[0] = high[0] - low[0];

        for (int i = 1; i < close.length; i++) {
            double highLow = high[i] - low[i];
            double highClose = Math.abs(high[i] - close[i - 1]);
            double lowClose = Math.abs(low[i] - close[i - 1]);
            tr[i] = Math.max(highLow, Math.max(highClose, lowClose));
        }

        // Fill early values with simple average
        for (int i = 0; i < period && i < close.length; i++) {
            double sum = 0;
            for (int j = 0; j <= i; j++) {
                sum += tr[j];
            }
            atr[i] = sum / (i + 1);
        }

        // Calculate ATR for remaining values
        for (int i = period; i < close.length; i++) {
            atr[i] = ((atr[i - 1] * (period - 1)) + tr[i]) / period;
        }
        return atr;
    }

    public static double[][] calculateStochasticOscillator(double[] close, double[] high, double[] low, int period) {
        double[] k = new double[close.length];
        double[] d = new double[close.length];

        for (int i = 0; i < close.length; i++) {
            // Use available data, not fixed period
            int lookback = Math.min(period, i + 1);
            
            double highestHigh = Double.NEGATIVE_INFINITY;
            double lowestLow = Double.POSITIVE_INFINITY;
            
            for (int j = 0; j < lookback; j++) {
                int index = i - j;
                if (index >= 0) {
                    if (high[index] > highestHigh) {
                        highestHigh = high[index];
                    }
                    if (low[index] < lowestLow) {
                        lowestLow = low[index];
                    }
                }
            }
            
            if (highestHigh == lowestLow) {
                k[i] = 50.0; // Avoid division by zero
            } else {
                k[i] = ((close[i] - lowestLow) / (highestHigh - lowestLow)) * 100;
            }
            
            // Calculate %D (3-period SMA of %K)
            if (i >= 2) {
                d[i] = (k[i] + k[i-1] + k[i-2]) / 3;
            } else if (i >= 1) {
                d[i] = (k[i] + k[i-1]) / 2;
            } else {
                d[i] = k[i];
            }
        }
        
        return new double[][]{k, d};
    }

    public static double[][] calculate(double[][] stockData, int emaPeriod, int rsiPeriod) {
        int priceIndex = 1;
        int highIndex = 2;
        int lowIndex = 3;

        double[] prices = new double[stockData.length];
        double[] highs = new double[stockData.length];
        double[] lows = new double[stockData.length];

        for (int i = 0; i < stockData.length; i++) {
            prices[i] = stockData[i][priceIndex];   // close
            highs[i] = stockData[i][highIndex];     // high
            lows[i] = stockData[i][lowIndex];       // low
        }

        // Calculate all technical indicators
        double[] ema = calculateEMA(prices, emaPeriod);
        double[] sma = calculateSMA(prices, 20);
        double[] rsi = calculateRSI(prices, rsiPeriod);
        double[] atr = calculateATR(highs, lows, prices, 14);
        double[][] macd = calculateMACD(prices, 12, 26, 9);
        double[][] bollingerBands = calculateBollingerBands(prices, 20, 2.0);
        double[][] stochastic = calculateStochasticOscillator(prices, highs, lows, 14);

        // Create extended indicators array with all 12 indicators
        double[][] indicators = new double[stockData.length][12];

        for (int i = 0; i < stockData.length; i++) {
            // Basic indicators
            indicators[i][0] = Double.isFinite(ema[i]) ? ema[i] : prices[i];
            indicators[i][1] = Double.isFinite(sma[i]) ? sma[i] : prices[i];
            indicators[i][2] = Double.isFinite(rsi[i]) ? Math.max(0, Math.min(100, rsi[i])) : 50.0;
            indicators[i][3] = Double.isFinite(atr[i]) ? atr[i] : 0.0;
            
            // MACD (3 values: macd, signal, histogram)
            indicators[i][4] = Double.isFinite(macd[0][i]) ? macd[0][i] : 0.0;
            indicators[i][5] = Double.isFinite(macd[1][i]) ? macd[1][i] : 0.0;
            indicators[i][6] = Double.isFinite(macd[2][i]) ? macd[2][i] : 0.0;
            
            // Bollinger Bands (3 values: sma, upper, lower)
            indicators[i][7] = Double.isFinite(bollingerBands[0][i]) ? bollingerBands[0][i] : prices[i];
            indicators[i][8] = Double.isFinite(bollingerBands[1][i]) ? bollingerBands[1][i] : prices[i];
            indicators[i][9] = Double.isFinite(bollingerBands[2][i]) ? bollingerBands[2][i] : prices[i];
            
            // Stochastic Oscillator (2 values: %K, %D)
            indicators[i][10] = Double.isFinite(stochastic[0][i]) ? Math.max(0, Math.min(100, stochastic[0][i])) : 50.0;
            indicators[i][11] = Double.isFinite(stochastic[1][i]) ? Math.max(0, Math.min(100, stochastic[1][i])) : 50.0;
        }

        return indicators;
    }
}
