package com.practice.paymentassignment.dto;

import com.practice.paymentassignment.entity.Payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class PaymentPrepareResponse {
    private final String merchantName;
    private final BigDecimal amount;
    private final Long paymentId;

    public static PaymentPrepareResponse from(Payment payment) {
        return new PaymentPrepareResponse(payment.getMerchant().getMerchantName(), payment.getAmount(),
                payment.getId());
    }
}
