package com.stockmanagment.porfoliomanagment.controller.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.HistockData;
import com.stockmanagment.porfoliomanagment.service.nepse.HistockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/histock-data")
public class HistockDataController {

    @Autowired
    private HistockDataService histockDataService;

    @GetMapping
    public List<HistockData> getAllHistockData() {
        return histockDataService.getAllHistockData();
    }

    @GetMapping("/{id}")
    public Optional<HistockData> getHistockDataById(@PathVariable int id) {
        return histockDataService.getHistockDataById(id);
    }

    @PostMapping
    public HistockData createHistockData(@RequestBody HistockData histockData) {
        return histockDataService.saveHistockData(histockData);
    }

    @PutMapping("/{id}")
    public HistockData updateHistockData(@PathVariable int id, @RequestBody HistockData histockData) {
        histockData.setId(id);
        return histockDataService.saveHistockData(histockData);
    }

    @DeleteMapping("/{id}")
    public void deleteHistockData(@PathVariable int id) {
        histockDataService.deleteHistockData(id);
    }
}
