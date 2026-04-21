package com.practice.paymentassignment.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
public class PaymentRequest {
    private Long paymentId;
    private Long merchantId;
    private BigDecimal amount;

    public PaymentRequest( Long paymentId, Long merchantId, BigDecimal amount) {
        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.amount = amount;
    }
}
