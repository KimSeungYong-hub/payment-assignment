package com.practice.paymentassignment.domain.user.repository;

import com.practice.paymentassignment.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
