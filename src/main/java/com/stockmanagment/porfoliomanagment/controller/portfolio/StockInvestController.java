package com.stockmanagment.porfoliomanagment.controller.portfolio;

import com.stockmanagment.porfoliomanagment.model.portfolio.StockInvest;
import com.stockmanagment.porfoliomanagment.service.portfolio.StockInvestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/stock-invests")
public class StockInvestController {

    @Autowired
    private StockInvestService stockInvestService;

    @GetMapping
    public List<StockInvest> getAllStockInvests() {
        return stockInvestService.getAllStockInvests();
    }

    @GetMapping("/{id}")
    public Optional<StockInvest> getStockInvestById(@PathVariable int id) {
        return stockInvestService.getStockInvestById(id);
    }

    @PostMapping
    public StockInvest createStockInvest(@RequestBody StockInvest stockInvest) {
        return stockInvestService.saveStockInvest(stockInvest);
    }

    @PutMapping("/{id}")
    public StockInvest updateStockInvest(@PathVariable int id, @RequestBody StockInvest stockInvest) {
        stockInvest.setId(id);
        return stockInvestService.saveStockInvest(stockInvest);
    }

    @DeleteMapping("/{id}")
    public void deleteStockInvest(@PathVariable int id) {
        stockInvestService.deleteStockInvest(id);
    }
}