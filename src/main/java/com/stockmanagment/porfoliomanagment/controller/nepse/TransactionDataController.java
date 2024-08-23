package com.stockmanagment.porfoliomanagment.controller.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.TransactionData;
import com.stockmanagment.porfoliomanagment.service.nepse.TransactionDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/transaction-data")
public class TransactionDataController {

    @Autowired
    private TransactionDataService transactionDataService;

    @GetMapping
    public List<TransactionData> getAllTransactionData() {
        return transactionDataService.getAllTransactionData();
    }

    @GetMapping("/{id}")
    public Optional<TransactionData> getTransactionDataById(@PathVariable int id) {
        return transactionDataService.getTransactionDataById(id);
    }

    @PostMapping
    public TransactionData createTransactionData(@RequestBody TransactionData transactionData) {
        return transactionDataService.saveTransactionData(transactionData);
    }

    @PutMapping("/{id}")
    public TransactionData updateTransactionData(@PathVariable int id, @RequestBody TransactionData transactionData) {
        transactionData.setId(id);
        return transactionDataService.saveTransactionData(transactionData);
    }

    @DeleteMapping("/{id}")
    public void deleteTransactionData(@PathVariable int id) {
        transactionDataService.deleteTransactionData(id);
    }
}
