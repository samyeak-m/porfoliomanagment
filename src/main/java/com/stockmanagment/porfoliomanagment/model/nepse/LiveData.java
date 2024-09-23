package com.stockmanagment.porfoliomanagment.model.nepse;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "live_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"date", "symbol"})
})
public class LiveData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private LocalDate date; // Changed to LocalDate
    private String symbol;

    
    private Double ltp;

    private String pointChange;
    private String perChange;

    
    private Double open;

    
    private Double high;

    
    private Double low;

    
    private Double prevClose;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Double getLtp() {
        return ltp;
    }

    public void setLtp(Double ltp) {
        this.ltp = ltp;
    }

    public String getPointChange() {
        return pointChange;
    }

    public void setPointChange(String pointChange) {
        this.pointChange = pointChange;
    }

    public String getPerChange() {
        return perChange;
    }

    public void setPerChange(String perChange) {
        this.perChange = perChange;
    }

    public Double getOpen() {
        return open;
    }

    public void setOpen(Double open) {
        this.open = open;
    }

    public Double getHigh() {
        return high;
    }

    public void setHigh(Double high) {
        this.high = high;
    }

    public Double getLow() {
        return low;
    }

    public void setLow(Double low) {
        this.low = low;
    }

    public Double getPrevClose() {
        return prevClose;
    }

    public void setPrevClose(Double prevClose) {
        this.prevClose = prevClose;
    }
}
