package com.stockmanagment.porfoliomanagment.model;

import java.time.LocalDate;

public class Prediction {
    private String stockSymbol;
    private double prediction;
    private double lastClose;
    private double pointChange;
    private double priceChange;
    private LocalDate predictionDate;

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    public double getPrediction() {
        return prediction;
    }

    public void setPrediction(double prediction) {
        this.prediction = prediction;
    }

    public double getLastClose() {
        return lastClose;
    }

    public void setLastClose(double lastClose) {
        this.lastClose = lastClose;
    }

    public double getPointChange() {
        return pointChange;
    }

    public void setPointChange(double pointChange) {
        this.pointChange = pointChange;
    }

    public double getPriceChange() {
        return priceChange;
    }

    public void setPriceChange(double priceChange) {
        this.priceChange = priceChange;
    }

    public LocalDate getPredictionDate() {
        return predictionDate;
    }

    public void setPredictionDate(LocalDate predictionDate) {
        this.predictionDate = predictionDate;
    }
}
