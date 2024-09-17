package com.stockmanagment.porfoliomanagment.model.nepse;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.sql.Timestamp;


@Entity
@Table(name = "daily_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"date", "symbol"})
})
public class DailyData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "date")
    private Timestamp date;
    private String symbol;
    private BigDecimal conf;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private String vwap;
    private BigDecimal vol;
    private BigDecimal prevClose;
    private BigDecimal turnover;
    private Integer trans;
    private String diff;

    @Column(name = "`range`")
    private BigDecimal range;

    private String diffPerc;
    private BigDecimal rangePerc;
    private String vwapPerc;
    private BigDecimal days120;
    private BigDecimal days180;
    private BigDecimal weeks52High;
    private BigDecimal weeks52Low;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getConf() {
        return conf;
    }

    public void setConf(BigDecimal conf) {
        this.conf = conf;
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

    public BigDecimal getClose() {
        return close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }

    public String getVwap() {
        return vwap;
    }

    public void setVwap(String vwap) {
        this.vwap = vwap;
    }

    public BigDecimal getVol() {
        return vol;
    }

    public void setVol(BigDecimal vol) {
        this.vol = vol;
    }

    public BigDecimal getPrevClose() {
        return prevClose;
    }

    public void setPrevClose(BigDecimal prevClose) {
        this.prevClose = prevClose;
    }

    public BigDecimal getTurnover() {
        return turnover;
    }

    public void setTurnover(BigDecimal turnover) {
        this.turnover = turnover;
    }

    public Integer getTrans() {
        return trans;
    }

    public void setTrans(Integer trans) {
        this.trans = trans;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public BigDecimal getRange() {
        return range;
    }

    public void setRange(BigDecimal range) {
        this.range = range;
    }

    public String getDiffPerc() {
        return diffPerc;
    }

    public void setDiffPerc(String diffPerc) {
        this.diffPerc = diffPerc;
    }

    public BigDecimal getRangePerc() {
        return rangePerc;
    }

    public void setRangePerc(BigDecimal rangePerc) {
        this.rangePerc = rangePerc;
    }

    public String getVwapPerc() {
        return vwapPerc;
    }

    public void setVwapPerc(String vwapPerc) {
        this.vwapPerc = vwapPerc;
    }

    public BigDecimal getDays120() {
        return days120;
    }

    public void setDays120(BigDecimal days120) {
        this.days120 = days120;
    }

    public BigDecimal getDays180() {
        return days180;
    }

    public void setDays180(BigDecimal days180) {
        this.days180 = days180;
    }

    public BigDecimal getWeeks52High() {
        return weeks52High;
    }

    public void setWeeks52High(BigDecimal weeks52High) {
        this.weeks52High = weeks52High;
    }

    public BigDecimal getWeeks52Low() {
        return weeks52Low;
    }

    public void setWeeks52Low(BigDecimal weeks52Low) {
        this.weeks52Low = weeks52Low;
    }
}
