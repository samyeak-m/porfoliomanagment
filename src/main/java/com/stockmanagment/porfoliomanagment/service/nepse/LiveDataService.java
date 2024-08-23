package com.stockmanagment.porfoliomanagment.service.nepse;

import com.stockmanagment.porfoliomanagment.model.nepse.LiveData;
import com.stockmanagment.porfoliomanagment.repository.nepse.LiveDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LiveDataService {

    @Autowired
    private LiveDataRepository liveDataRepository;

    public List<LiveData> getAllLiveData() {
        return liveDataRepository.findAll();
    }

    public Optional<LiveData> getLiveDataById(int id) {
        return liveDataRepository.findById(id);
    }

    public LiveData saveLiveData(LiveData liveData) {
        return liveDataRepository.save(liveData);
    }

    public void deleteLiveData(int id) {
        liveDataRepository.deleteById(id);
    }
}