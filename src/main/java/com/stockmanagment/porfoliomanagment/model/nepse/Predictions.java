package com.stockmanagment.porfoliomanagment.model.nepse;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "predictions")
public class Predictions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String stockSymbol;

    
    private Double pointChange;

    
    private Double priceChange;

    
    private Double prediction;

    
    private Double lastClose;

    private LocalDate predictionDate; // Changed to LocalDate

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    public Double getPointChange() {
        return pointChange;
    }

    public void setPointChange(Double pointChange) {
        this.pointChange = pointChange;
    }

    public Double getPriceChange() {
        return priceChange;
    }

    public void setPriceChange(Double priceChange) {
        this.priceChange = priceChange;
    }

    public Double getPrediction() {
        return prediction;
    }

    public void setPrediction(Double prediction) {
        this.prediction = prediction;
    }

    public Double getLastClose() {
        return lastClose;
    }

    public void setLastClose(Double lastClose) {
        this.lastClose = lastClose;
    }

    public LocalDate getPredictionDate() {
        return predictionDate;
    }

    public void setPredictionDate(LocalDate predictionDate) {
        this.predictionDate = predictionDate;
    }
}
