package com.practice.paymentassignment.domain.merchant;

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

    @Column(length = 100, nullable = false)
    private String merchantName;

    // 컬럼명이 명확하지 않습니다. amount가 구체적으로 무엇을 의미하는지 컬럼명만 보고 알 수 있어야합니다.
    @Column( precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Builder
    public Merchant(String merchantName) {
        this.merchantName = merchantName;
        this.amount = BigDecimal.valueOf(10000);
    }
}
