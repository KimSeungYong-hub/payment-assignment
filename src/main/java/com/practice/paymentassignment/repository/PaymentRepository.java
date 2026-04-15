package com.practice.paymentassignment.repository;

import com.practice.paymentassignment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByOrderId(String orderId);

    Payment findByOrderId(String orderId);
}
