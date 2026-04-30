package com.practice.paymentassignment.domain.merchant.repository;

import com.practice.paymentassignment.domain.merchant.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
}
