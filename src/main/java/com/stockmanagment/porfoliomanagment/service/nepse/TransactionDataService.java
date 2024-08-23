package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.TransactionData;
import com.stockmanagment.porfoliomanagment.repository.nepse.TransactionDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TransactionDataService {

    @Autowired
    private TransactionDataRepository transactionDataRepository;

    public List<TransactionData> getAllTransactionData() {
        return transactionDataRepository.findAll();
    }

    public Optional<TransactionData> getTransactionDataById(int id) {
        return transactionDataRepository.findById(id);
    }

    public TransactionData saveTransactionData(TransactionData transactionData) {
        return transactionDataRepository.save(transactionData);
    }

    public void deleteTransactionData(int id) {
        transactionDataRepository.deleteById(id);
    }
}