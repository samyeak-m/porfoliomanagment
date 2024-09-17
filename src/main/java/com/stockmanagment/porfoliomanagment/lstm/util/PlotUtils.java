package com.stockmanagment.porfoliomanagment.lstm.util;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PlotUtils {

    public static void savePredictionPlot(String stockSymbol, List<double[]> stockData, List<Double> predictions) {
        XYSeries seriesActual = new XYSeries("Actual");
        XYSeries seriesPredicted = new XYSeries("Predicted");

        for (int i = 0; i < stockData.size(); i++) {
            seriesActual.add(i, stockData.get(i)[1]); // Actual close price
            if (i >= stockData.size() - predictions.size()) {
                seriesPredicted.add(i, predictions.get(i - (stockData.size() - predictions.size())));
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(seriesActual);
        dataset.addSeries(seriesPredicted);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Stock Price Prediction for " + stockSymbol,
                "Time",
                "Price",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        // Save the chart as a PNG
        String filePath = "predictions/" + stockSymbol + "_prediction.png";
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try {
            ChartUtils.saveChartAsPNG(file, chart, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
