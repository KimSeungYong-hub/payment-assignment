package com.practice.paymentassignment.entity;

import jakarta.persistence.*;
import jdk.jshell.Snippet;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private int amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(unique=true)
    private String orderId;

    private OffsetDateTime dateTime;

    @Builder
    public Payment( Merchant merchant, int amount, User user, PaymentStatus status, String orderId) {
        this.merchant = merchant;
        this.amount = amount;
        this.user = user;
        this.status = status;
        this.orderId = orderId;
    }


    public void complete(){
        this.status = PaymentStatus.SUCCESS;
    }

    public void cancel(){
        this.status = PaymentStatus.CANCELED;
    }

}
