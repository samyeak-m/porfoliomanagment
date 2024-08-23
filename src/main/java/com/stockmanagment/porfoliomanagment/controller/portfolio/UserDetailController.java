package com.stockmanagment.porfoliomanagment.controller.portfolio;

import com.stockmanagment.porfoliomanagment.model.portfolio.UserDetail;
import com.stockmanagment.porfoliomanagment.service.portfolio.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user-details")
public class UserDetailController {

    @Autowired
    private UserDetailService userDetailService;

    @GetMapping
    public List<UserDetail> getAllUserDetails() {
        return userDetailService.getAllUserDetails();
    }

    @GetMapping("/{id}")
    public Optional<UserDetail> getUserDetailById(@PathVariable int id) {
        return userDetailService.getUserDetailById(id);
    }

    @PostMapping
    public UserDetail createUserDetail(@RequestBody UserDetail userDetail) {
        return userDetailService.saveUserDetail(userDetail);
    }

    @PutMapping("/{id}")
    public UserDetail updateUserDetail(@PathVariable int id, @RequestBody UserDetail userDetail) {
        userDetail.setId(id);
        return userDetailService.saveUserDetail(userDetail);
    }

    @DeleteMapping("/{id}")
    public void deleteUserDetail(@PathVariable int id) {
        userDetailService.deleteUserDetail(id);
    }
}