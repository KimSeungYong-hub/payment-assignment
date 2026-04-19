package com.practice.paymentassignment.dto;

import com.practice.paymentassignment.entity.Payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class PaymentInfoResponse {
    private final String merchantName;
    private final BigDecimal amount;

    public static PaymentInfoResponse from(Payment payment) {
        return new PaymentInfoResponse(payment.getMerchant().getMerchantName(), payment.getAmount());
    }
}
