package com.stockmanagment.porfoliomanagment.controller.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.VarOfAllData;
import com.stockmanagment.porfoliomanagment.service.nepse.VarOfAllDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/varofall-data")
public class VarofallDataController {

    @Autowired
    private VarOfAllDataService varOfAllDataService;

    @GetMapping
    public List<VarOfAllData> getAllVarOfAllData() {
        return varOfAllDataService.getAllVarOfAllData();
    }

    @GetMapping("/{id}")
    public Optional<VarOfAllData> getVarOfAllDataById(@PathVariable int id) {
        return varOfAllDataService.getVarOfAllDataById(id);
    }

    @PostMapping
    public VarOfAllData createVarOfAllData(@RequestBody VarOfAllData varOfAllData) {
        return varOfAllDataService.saveVarOfAllData(varOfAllData);
    }

    @PutMapping("/{id}")
    public VarOfAllData updateVarOfAllData(@PathVariable int id, @RequestBody VarOfAllData varOfAllData) {
        varOfAllData.setId(id);
        return varOfAllDataService.saveVarOfAllData(varOfAllData);
    }

    @DeleteMapping("/{id}")
    public void deleteVarOfAllData(@PathVariable int id) {
        varOfAllDataService.deleteVarOfAllData(id);
    }
}
