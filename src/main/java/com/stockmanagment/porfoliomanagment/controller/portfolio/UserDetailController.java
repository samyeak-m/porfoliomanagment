package com.stockmanagment.porfoliomanagment.controller.portfolio;

import com.stockmanagment.porfoliomanagment.model.portfolio.UserDetail;
import com.stockmanagment.porfoliomanagment.service.portfolio.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/api/auth")
public class UserDetailController {

    @Autowired
    private UserDetailService userDetailService;

    public UserDetailController(UserDetailService userDetailService) {
        this.userDetailService = userDetailService;
    }

    @GetMapping("/user-details")
    public List<UserDetail> getAllUserDetails() {
        return userDetailService.getAllUserDetails();
    }

    @GetMapping("/user-details/{id}")
    public ResponseEntity<UserDetail> getUserDetailById(@PathVariable int id) {
        Optional<UserDetail> userDetail = userDetailService.getUserDetailById(id);
        return userDetail.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDetail> createUserDetail(@RequestBody UserDetail userDetail) {
        UserDetail createdUser = userDetailService.saveUserDetail(userDetail);
        return ResponseEntity.ok(createdUser);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String email, @RequestParam String password) {
        Optional<UserDetail> userDetail = userDetailService.login(email, password);
        if (userDetail.isPresent()) {
//            session.setAttribute("user", userDetail.get());
            return ResponseEntity.ok("Login successful");
        }
        return ResponseEntity.status(401).body("Invalid email or password");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logout successful");
    }

    @PutMapping("/user-details/{id}")
    public ResponseEntity<UserDetail> updateUserDetail(@PathVariable int id, @RequestBody UserDetail userDetail) {
        userDetail.setId(id);
        UserDetail updatedUser = userDetailService.saveUserDetail(userDetail);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/user-details/{id}")
    public ResponseEntity<Void> deleteUserDetail(@PathVariable int id) {
        userDetailService.deleteUserDetail(id);
        return ResponseEntity.noContent().build();
    }
}
