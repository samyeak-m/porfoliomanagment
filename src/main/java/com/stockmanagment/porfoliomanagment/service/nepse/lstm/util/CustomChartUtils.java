package com.stockmanagment.porfoliomanagment.service.nepse.lstm.util;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class CustomChartUtils {


    public static void saveAccuracyChart(String title, List<Integer> epochs, List<Double> accuracy, List<Double> validationAccuracy, String filePath, String xAxisLabel, String yAxisLabel, int interval) {
        XYSeries accuracySeries = new XYSeries("Training Accuracy");
        XYSeries validationAccuracySeries = new XYSeries("Validation Accuracy");

        for (int i = 0; i < epochs.size(); i++) {
            accuracySeries.add((double)epochs.get(i), accuracy.get(i));
            validationAccuracySeries.add((double)epochs.get(i), validationAccuracy.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(accuracySeries);
        dataset.addSeries(validationAccuracySeries);

        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);

        XYPlot plot = chart.getXYPlot();
        NumberAxis xAxis = new NumberAxis(xAxisLabel);
        xAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(interval));
        plot.setDomainAxis(xAxis);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesLinesVisible(1, true);
        renderer.setSeriesShapesVisible(1, false);
        renderer.setSeriesPaint(0, Color.GREEN);
        renderer.setSeriesPaint(1, Color.RED);
        plot.setRenderer(renderer);

        saveChart(chart, filePath);
    }

    public static void saveLossChart(String title, List<Integer> epochs, List<Double> loss, List<Double> validationLoss, String filePath, String xAxisLabel, String yAxisLabel, int interval) {
        XYSeries lossSeries = new XYSeries("Training Loss");
        XYSeries validationLossSeries = new XYSeries("Validation Loss");

        for (int i = 0; i < epochs.size(); i++) {
            lossSeries.add((double)epochs.get(i), loss.get(i));
            validationLossSeries.add((double)epochs.get(i), validationLoss.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(lossSeries);
        dataset.addSeries(validationLossSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);

        XYPlot plot = chart.getXYPlot();
        NumberAxis xAxis = new NumberAxis(xAxisLabel);
        xAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(interval));
        plot.setDomainAxis(xAxis);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesLinesVisible(1, true);
        renderer.setSeriesShapesVisible(1, false);
        renderer.setSeriesPaint(0, Color.GREEN);
        renderer.setSeriesPaint(1, Color.RED);
        plot.setRenderer(renderer);

        saveChart(chart, filePath);
    }

    private static double findMaxInRange(List<Double> data, int start, int end) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = start; i < end; i++) {
            if (data.get(i) > max) {
                max = data.get(i);
            }
        }
        return max;
    }

    private static void saveChart(JFreeChart chart, String filePath) {
        File chartFile = new File(filePath);
        try {
            ChartUtils.saveChartAsPNG(chartFile, chart, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
