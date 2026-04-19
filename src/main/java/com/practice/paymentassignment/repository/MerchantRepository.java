package com.practice.paymentassignment.repository;

import com.practice.paymentassignment.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
}
