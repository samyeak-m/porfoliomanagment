package com.stockmanagment.porfoliomanagment.model.nepse;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Stock {

    @Id
    private Long id;
    private String symbol;
    private Double investmentAmount;
    private Double price;
    @ElementCollection
    @CollectionTable(name = "close_prices")
    @Column(name = "price")
    private List<Double> closePrices;

    @ElementCollection
    @CollectionTable(name = "historical_prices")
    @Column(name = "price")
    private List<Double> historicalPrices;

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
