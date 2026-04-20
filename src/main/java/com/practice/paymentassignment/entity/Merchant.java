package com.practice.paymentassignment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String merchantName;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Builder
    public Merchant(String merchantName) {
        this.merchantName = merchantName;
        this.amount = BigDecimal.valueOf(10000);
    }
}
