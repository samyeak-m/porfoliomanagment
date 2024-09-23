package com.stockmanagment.porfoliomanagment.model.nepse;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"date", "symbol"})
})
public class DailyData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "conf")
    private double conf;

    @Column(name = "open")
    private double open;

    @Column(name = "high")
    private double high;

    @Column(name = "low")
    private double low;

    @Column(name = "close")
    private double close;

    @Column(name = "vwap")
    private String vwap;

    @Column(name = "vol")
    private double vol;

    @Column(name = "prev_close")
    private double prev_Close;

    @Column(name = "turnover")
    private double turnover;

    @Column(name = "trans")
    private Integer trans;

    @Column(name = "diff")
    private String diff;

    @Column(name = "`range`")
    private double range;

    @Column(name = "diff_perc")
    private String diff_Perc;

    @Column(name = "range_perc")
    private double range_Perc;

    @Column(name = "vwap_perc")
    private String vwap_Perc;

    @Column(name = "days_120")
    private double days_120;

    @Column(name = "days_180")
    private double days_180;

    @Column(name = "weeks_52_high")
    private double weeks_52_High;

    @Column(name = "weeks_52_low")
    private double weeks_52_Low;

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

    public double getConf() {
        return conf;
    }

    public void setConf(double conf) {
        this.conf = conf;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public String getVwap() {
        return vwap;
    }

    public void setVwap(String vwap) {
        this.vwap = vwap;
    }

    public double getVol() {
        return vol;
    }

    public void setVol(double vol) {
        this.vol = vol;
    }

    public double getPrev_Close() {
        return prev_Close;
    }

    public void setPrev_Close(double prev_Close) {
        this.prev_Close = prev_Close;
    }

    public double getTurnover() {
        return turnover;
    }

    public void setTurnover(double turnover) {
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

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public String getDiff_Perc() {
        return diff_Perc;
    }

    public void setDiff_Perc(String diff_Perc) {
        this.diff_Perc = diff_Perc;
    }

    public double getRange_Perc() {
        return range_Perc;
    }

    public void setRange_Perc(double range_Perc) {
        this.range_Perc = range_Perc;
    }

    public String getVwap_Perc() {
        return vwap_Perc;
    }

    public void setVwap_Perc(String vwap_Perc) {
        this.vwap_Perc = vwap_Perc;
    }

    public double getDays_120() {
        return days_120;
    }

    public void setDays_120(double days_120) {
        this.days_120 = days_120;
    }

    public double getDays_180() {
        return days_180;
    }

    public void setDays_180(double days_180) {
        this.days_180 = days_180;
    }

    public double getWeeks_52_High() {
        return weeks_52_High;
    }

    public void setWeeks_52_High(double weeks_52_High) {
        this.weeks_52_High = weeks_52_High;
    }

    public double getWeeks_52_Low() {
        return weeks_52_Low;
    }

    public void setWeeks_52_Low(double weeks_52_Low) {
        this.weeks_52_Low = weeks_52_Low;
    }
}
