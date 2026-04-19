package com.practice.paymentassignment.entity;

import jakarta.persistence.*;
import jdk.jshell.Snippet;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(unique = true)
    private String orderId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateTime;

    @Builder
    public Payment(Merchant merchant, BigDecimal amount, Wallet wallet, PaymentStatus status, String orderId) {
        this.merchant = merchant;
        this.amount = amount;
        this.wallet = wallet;
        this.status = status;
        this.orderId = orderId;
    }

    public void complete() {
        this.status = PaymentStatus.SUCCESS;
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELED;
    }

}
