package com.practice.paymentassignment.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
public class PaymentRequest {
    private Long userId;
    private Long merchantId;
    private Long paymentId;
    private String merchantName;
    private int amount;
    private String orderId;

    public PaymentRequest(Long userId, Long merchantId,Long paymentId,String merchantName, int amount, String orderId) {
        this.userId = userId;
        this.merchantId = merchantId;
        this.paymentId = paymentId;
        this.merchantName = merchantName;
        this.amount = amount;
        this.orderId = orderId;
    }
}
