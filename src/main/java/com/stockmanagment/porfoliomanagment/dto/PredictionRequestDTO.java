package com.stockmanagment.porfoliomanagment.dto;

public class PredictionRequestDTO {
    private String stockSymbol;
    private Integer daysOfInvestment = 25;
    private String confidenceLevel = "0.95";
    private String simulationType = "monte_carlo";
    private Integer numSimulations = 10000;
    
    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
    
    public Integer getDaysOfInvestment() {
        return daysOfInvestment;
    }
    
    public void setDaysOfInvestment(Integer daysOfInvestment) {
        this.daysOfInvestment = daysOfInvestment;
    }
    
    public String getConfidenceLevel() {
        return confidenceLevel;
    }
    
    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }
    
    public String getSimulationType() {
        return simulationType;
    }
    
    public void setSimulationType(String simulationType) {
        this.simulationType = simulationType;
    }
    
    public Integer getNumSimulations() {
        return numSimulations;
    }
    
    public void setNumSimulations(Integer numSimulations) {
        this.numSimulations = numSimulations;
    }
}
