package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.VarOfAllData;
import com.stockmanagment.porfoliomanagment.repository.nepse.VarOfAllDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VarOfAllDataService {

    @Autowired
    private VarOfAllDataRepository varOfAllDataRepository;

    public List<VarOfAllData> getAllVarOfAllData() {
        return varOfAllDataRepository.findAll();
    }

    public Optional<VarOfAllData> getVarOfAllDataById(int id) {
        return varOfAllDataRepository.findById(id);
    }

    public VarOfAllData saveVarOfAllData(VarOfAllData varOfAllData) {
        return varOfAllDataRepository.save(varOfAllData);
    }

    public void deleteVarOfAllData(int id) {
        varOfAllDataRepository.deleteById(id);
    }
}