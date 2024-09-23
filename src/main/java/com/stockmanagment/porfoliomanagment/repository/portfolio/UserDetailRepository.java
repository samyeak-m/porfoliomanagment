package com.stockmanagment.porfoliomanagment.repository.portfolio;

import com.stockmanagment.porfoliomanagment.model.portfolio.UserDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDetailRepository extends JpaRepository<UserDetail, Integer> {
    UserDetail findByEmail(String email);
}
