package com.practice.paymentassignment.domain.payment;

import com.practice.paymentassignment.global.entity.BaseEntity;
import com.practice.paymentassignment.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private PaymentRequest paymentRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User user;

    @Column(precision = 19, scale = 0, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(length = 500)
    private String failReason;

    @Builder
    public Payment(BigDecimal amount, PaymentStatus status, User user, PaymentRequest paymentRequest, String failReason) {
        this.amount = amount;
        this.status = status;
        this.paymentRequest = paymentRequest;
        this.user = user;
        this.failReason = failReason;
    }

    public static Payment createSuccess(PaymentRequest paymentRequest, User user, BigDecimal amount) {
        return Payment.builder()
                .paymentRequest(paymentRequest)
                .user(user)
                .amount(amount)
                .status(PaymentStatus.SUCCESS)
                .build();
    }

    public static Payment createFail(PaymentRequest paymentRequest, User user, BigDecimal amount, String reason) {
        return Payment.builder()
                .paymentRequest(paymentRequest)
                .user(user)
                .amount(amount)
                .status(PaymentStatus.FAIL)
                .failReason(reason)
                .build();
    }


}
