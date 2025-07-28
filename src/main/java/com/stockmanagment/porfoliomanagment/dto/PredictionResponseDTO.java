package com.stockmanagment.porfoliomanagment.dto;

public class PredictionResponseDTO {
    private String stockSymbol;
    private double prediction;
    private double lastClose;
    private double pointChange;
    private double priceChange;
    private String predictionDate;
    
    // Getters and setters
    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
    
    public double getPrediction() { return prediction; }
    public void setPrediction(double prediction) { this.prediction = prediction; }
    
    public double getLastClose() { return lastClose; }
    public void setLastClose(double lastClose) { this.lastClose = lastClose; }
    
    public double getPointChange() { return pointChange; }
    public void setPointChange(double pointChange) { this.pointChange = pointChange; }
    
    public double getPriceChange() { return priceChange; }
    public void setPriceChange(double priceChange) { this.priceChange = priceChange; }
    
    public String getPredictionDate() { return predictionDate; }
    public void setPredictionDate(String predictionDate) { this.predictionDate = predictionDate; }
}
