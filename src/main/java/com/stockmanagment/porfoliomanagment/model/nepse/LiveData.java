package com.stockmanagment.porfoliomanagment.model.nepse;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;

@Entity
@Table(name = "live_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"date", "symbol"})
})
public class LiveData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Date date;
    private String symbol;
    private BigDecimal ltp;
    private String pointChange;
    private String perChange;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal prevClose;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getLtp() {
        return ltp;
    }

    public void setLtp(BigDecimal ltp) {
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

    public BigDecimal getOpen() {
        return open;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getPrevClose() {
        return prevClose;
    }

    public void setPrevClose(BigDecimal prevClose) {
        this.prevClose = prevClose;
    }
}
