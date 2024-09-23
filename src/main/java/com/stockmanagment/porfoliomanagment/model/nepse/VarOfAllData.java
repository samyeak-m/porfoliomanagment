package com.stockmanagment.porfoliomanagment.model.nepse;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "varofall_data")
public class VarOfAllData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "stock_symbol")
    private String stockSymbol;

    @Column(name = "date")
    private LocalDateTime date; // Changed to LocalDateTime

    @Column(name = "days_of_investment")
    private int daysOfInvestment;

    @Column(name = "mean_return") // Precision and scale added
    private double meanReturn;

    @Column(name = "volatility") // Precision and scale added
    private double volatility;

    @Column(name = "var") // Precision and scale added
    private double var;

    @Column(name = "initial_stock_price") // Precision and scale added
    private double initialStockPrice;

    @Column(name = "confidence_level") // Precision and scale added
    private double confidenceLevel;

    // Getters and setters
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

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public int getDaysOfInvestment() {
        return daysOfInvestment;
    }

    public void setDaysOfInvestment(int daysOfInvestment) {
        this.daysOfInvestment = daysOfInvestment;
    }

    public double getMeanReturn() {
        return meanReturn;
    }

    public void setMeanReturn(double meanReturn) {
        this.meanReturn = meanReturn;
    }

    public double getVolatility() {
        return volatility;
    }

    public void setVolatility(double volatility) {
        this.volatility = volatility;
    }

    public double getVar() {
        return var;
    }

    public void setVar(double var) {
        this.var = var;
    }

    public double getInitialStockPrice() {
        return initialStockPrice;
    }

    public void setInitialStockPrice(double initialStockPrice) {
        this.initialStockPrice = initialStockPrice;
    }

    public double getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }
}
