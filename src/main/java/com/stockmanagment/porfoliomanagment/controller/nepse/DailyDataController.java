package com.stockmanagment.porfoliomanagment.controller.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.DailyData;
import com.stockmanagment.porfoliomanagment.service.nepse.DailyDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/daily-data")
public class DailyDataController {

    @Autowired
    private DailyDataService dailyDataService;

    @GetMapping
    public List<DailyData> getAllDailyData() {
        return dailyDataService.getAllDailyData();
    }

    @GetMapping("/{id}")
    public Optional<DailyData> getDailyDataById(@PathVariable int id) {
        return dailyDataService.getDailyDataById(id);
    }

    @PostMapping
    public DailyData createDailyData(@RequestBody DailyData dailyData) {
        return dailyDataService.saveDailyData(dailyData);
    }

    @PutMapping("/{id}")
    public DailyData updateDailyData(@PathVariable int id, @RequestBody DailyData dailyData) {
        dailyData.setId(id); // No need for explicit casting
        return dailyDataService.saveDailyData(dailyData);
    }

    @DeleteMapping("/{id}")
    public void deleteDailyData(@PathVariable int id) {
        dailyDataService.deleteDailyData(id);
    }
}
