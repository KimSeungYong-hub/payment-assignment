package com.practice.paymentassignment.dto;

import com.practice.paymentassignment.entity.Payment;

import com.practice.paymentassignment.entity.PaymentRequestEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class PaymentInfoResponse {
    private final String merchantName;
    private final BigDecimal amount;

    public static PaymentInfoResponse from(PaymentRequestEntity paymentRequest) {
        return new PaymentInfoResponse(paymentRequest.getMerchant().getMerchantName(), paymentRequest.getTotalAmount());
    }
}
