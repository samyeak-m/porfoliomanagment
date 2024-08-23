package com.stockmanagment.porfoliomanagment.model.portfolio;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "stock_invest")
public class StockInvest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "stock_name")
    private String stockName;

    @Column(name = "buy_price")
    private double buyPrice;

    @Temporal(TemporalType.DATE)
    @Column(name = "buy_date")
    private Date buyDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "hold_till")
    private Date holdTill;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserDetail user;

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public Date getBuyDate() {
        return buyDate;
    }

    public void setBuyDate(Date buyDate) {
        this.buyDate = buyDate;
    }

    public Date getHoldTill() {
        return holdTill;
    }

    public void setHoldTill(Date holdTill) {
        this.holdTill = holdTill;
    }

    public UserDetail getUser() {
        return user;
    }

    public void setUser(UserDetail user) {
        this.user = user;
    }
}
