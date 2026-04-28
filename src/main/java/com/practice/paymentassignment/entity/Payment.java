package com.practice.paymentassignment.entity;

import jakarta.persistence.*;
import jdk.jshell.Snippet;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_request_id", nullable = false)
    private PaymentRequestEntity paymentRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount", precision = 19, scale = 0, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @CreatedDate
//    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant dateTime;

    @Builder
    public Payment(BigDecimal amount, PaymentStatus status, User user, PaymentRequestEntity paymentRequestEntity, String failReason) {
        this.amount = amount;
        this.status = status;
        this.paymentRequest = paymentRequestEntity;
        this.user = user;
        this.failReason = failReason;
    }

    public static Payment createSuccess(PaymentRequestEntity paymentRequestEntity, User user, BigDecimal amount) {
        return Payment.builder()
                .paymentRequestEntity(paymentRequestEntity)
                .user(user)
                .amount(amount)
                .status(PaymentStatus.SUCCESS)
                .build();
    }

    public static Payment createFail(PaymentRequestEntity paymentRequestEntity, User user, BigDecimal amount, String reason) {
        return Payment.builder()
                .paymentRequestEntity(paymentRequestEntity)
                .user(user)
                .amount(amount)
                .status(PaymentStatus.FAIL)
                .failReason(reason)
                .build();
    }

//    public void complete() {
//        this.status = PaymentStatus.SUCCESS;
//    }
//
//    public void cancel() {
//        this.status = PaymentStatus.FAIL;
//    }

}
