package com.stockmanagment.porfoliomanagment.service.portfolio;

import com.stockmanagment.porfoliomanagment.model.portfolio.UserDetail;
import com.stockmanagment.porfoliomanagment.repository.portfolio.UserDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserDetailService {

    @Autowired
    private UserDetailRepository userDetailRepository;

    public List<UserDetail> getAllUserDetails() {
        return userDetailRepository.findAll();
    }

    public Optional<UserDetail> getUserDetailById(int id) {
        return userDetailRepository.findById(id);
    }

    public UserDetail saveUserDetail(UserDetail userDetail) {
        return userDetailRepository.save(userDetail);
    }

    public void deleteUserDetail(int id) {
        userDetailRepository.deleteById(id);
    }
}