package com.practice.paymentassignment.repository;

import com.practice.paymentassignment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
