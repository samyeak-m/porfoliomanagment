package com.stockmanagment.porfoliomanagment.service.portfolio;

import com.stockmanagment.porfoliomanagment.model.portfolio.StockInvest;
import com.stockmanagment.porfoliomanagment.repository.portfolio.StockInvestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StockInvestService {

    @Autowired
    private StockInvestRepository stockInvestRepository;

    public List<StockInvest> getAllStockInvests() {
        return stockInvestRepository.findAll();
    }

    public Optional<StockInvest> getStockInvestById(int id) {
        return stockInvestRepository.findById(id);
    }

    public StockInvest saveStockInvest(StockInvest stockInvest) {
        return stockInvestRepository.save(stockInvest);
    }

    public void deleteStockInvest(int id) {
        stockInvestRepository.deleteById(id);
    }
}