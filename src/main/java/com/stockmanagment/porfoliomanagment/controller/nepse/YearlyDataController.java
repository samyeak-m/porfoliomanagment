package com.stockmanagment.porfoliomanagment.controller.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.YearlyData;
import com.stockmanagment.porfoliomanagment.service.nepse.YearlyDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/yearly-data")
public class YearlyDataController {

    @Autowired
    private YearlyDataService yearlyDataService;

    @GetMapping
    public List<YearlyData> getAllYearlyData() {
        return yearlyDataService.getAllYearlyData();
    }

    @GetMapping("/{id}")
    public Optional<YearlyData> getYearlyDataById(@PathVariable int id) {
        return yearlyDataService.getYearlyDataById(id);
    }

    @PostMapping
    public YearlyData createYearlyData(@RequestBody YearlyData yearlyData) {
        return yearlyDataService.saveYearlyData(yearlyData);
    }

    @PutMapping("/{id}")
    public YearlyData updateYearlyData(@PathVariable int id, @RequestBody YearlyData yearlyData) {
        yearlyData.setId(id);
        return yearlyDataService.saveYearlyData(yearlyData);
    }

    @DeleteMapping("/{id}")
    public void deleteYearlyData(@PathVariable int id) {
        yearlyDataService.deleteYearlyData(id);
    }
}
