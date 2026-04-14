package com.practice.paymentassignment.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
public class PaymentRequest {
    private Long userId;
    private String merchantName;
    private int amount;

    public PaymentRequest(Long userId, String merchantName, int amount) {
        this.userId = userId;
        this.merchantName = merchantName;
        this.amount = amount;
    }
}
