// src/main/java/com/stockmanagment/porfoliomanagment/controller/nepse/VarDataController.java
package com.stockmanagment.porfoliomanagment.controller.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.VarData;
import com.stockmanagment.porfoliomanagment.service.nepse.VarDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/var-data")
public class VarDataController {

    @Autowired
    private VarDataService varDataService;

    @GetMapping
    public List<VarData> getAllVarData() {
        return varDataService.getAllVarData();
    }

    @GetMapping("/{id}")
    public Optional<VarData> getVarDataById(@PathVariable int id) {
        return varDataService.getVarDataById(id);
    }

    @PostMapping
    public VarData createVarData(@RequestBody VarData varData) {
        return varDataService.saveVarData(varData);
    }

    @PutMapping("/{id}")
    public VarData updateVarData(@PathVariable int id, @RequestBody VarData varData) {
        varData.setId(id);
        return varDataService.saveVarData(varData);
    }

    @DeleteMapping("/{id}")
    public void deleteVarData(@PathVariable int id) {
        varDataService.deleteVarData(id);
    }
}
