package com.stockmanagment.porfoliomanagment.controller.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.LiveData;
import com.stockmanagment.porfoliomanagment.service.nepse.LiveDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/live-data")
public class LiveDataController {

    @Autowired
    private LiveDataService liveDataService;

    @GetMapping
    public List<LiveData> getAllLiveData() {
        return liveDataService.getAllLiveData();
    }

    @GetMapping("/{id}")
    public Optional<LiveData> getLiveDataById(@PathVariable int id) {
        return liveDataService.getLiveDataById(id);
    }

    @PostMapping
    public LiveData createLiveData(@RequestBody LiveData liveData) {
        return liveDataService.saveLiveData(liveData);
    }

    @PutMapping("/{id}")
    public LiveData updateLiveData(@PathVariable int id, @RequestBody LiveData liveData) {
        liveData.setId(id);
        return liveDataService.saveLiveData(liveData);
    }

    @DeleteMapping("/{id}")
    public void deleteLiveData(@PathVariable int id) {
        liveDataService.deleteLiveData(id);
    }
}
