package com.stockmanagment.porfoliomanagment.model.nepse;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import java.util.List;

@Entity
public class Stock {

    @Id
    private Long id;
    private String symbol;
    private Double investmentAmount;
    private Double price;  // The current price of the stock

    @ElementCollection
    @CollectionTable(name = "close_prices")  // Specifies the table to store closePrices
    @Column(name = "price")
    private List<Double> closePrices;  // The list of close prices for recent days

    @ElementCollection
    @CollectionTable(name = "historical_prices")  // Specifies the table to store historicalPrices
    @Column(name = "price")
    private List<Double> historicalPrices;  // The list of historical prices

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Double getInvestmentAmount() {
        return investmentAmount;
    }

    public void setInvestmentAmount(Double investmentAmount) {
        this.investmentAmount = investmentAmount;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public List<Double> getClosePrices() {
        return closePrices;
    }

    public void setClosePrices(List<Double> closePrices) {
        this.closePrices = closePrices;
    }

    public List<Double> getHistoricalPrices() {
        return historicalPrices;
    }

    public void setHistoricalPrices(List<Double> historicalPrices) {
        this.historicalPrices = historicalPrices;
    }
}
