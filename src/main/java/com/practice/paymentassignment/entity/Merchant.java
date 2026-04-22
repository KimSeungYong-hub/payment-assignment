package com.practice.paymentassignment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "merchants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_name", length = 100, nullable = false)
    private String merchantName;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Builder
    public Merchant(String merchantName) {
        this.merchantName = merchantName;
        this.amount = BigDecimal.valueOf(10000);
    }
}
