package com.stockmanagment.porfoliomanagment.model.nepse;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "var_data")
public class VarData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String stockSymbol;
    private Timestamp date;
    private int daysOfInvestment;
    private double meanReturn;
    private double volatility;
    private double var;
    private double initialStockPrice;
    private double confidenceLevel;

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

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
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
