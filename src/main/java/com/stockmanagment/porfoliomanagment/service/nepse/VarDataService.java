package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.VarData;
import com.stockmanagment.porfoliomanagment.repository.nepse.VarDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VarDataService {

    @Autowired
    private VarDataRepository varDataRepository;

    public List<VarData> getAllVarData() {
        return varDataRepository.findAll();
    }

    public Optional<VarData> getVarDataById(int id) {
        return varDataRepository.findById(id);
    }

    public VarData saveVarData(VarData varData) {
        return varDataRepository.save(varData);
    }

    public void deleteVarData(int id) {
        varDataRepository.deleteById(id);
    }
}