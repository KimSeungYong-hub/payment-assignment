package com.practice.paymentassignment.dto;

import com.practice.paymentassignment.entity.Payment;

import com.practice.paymentassignment.entity.PaymentRequestEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class PaymentPrepareResponse {
    private final String merchantName;
    private final BigDecimal amount;
    private final Long paymentId;

    public static PaymentPrepareResponse from(PaymentRequestEntity paymentRequest) {
        return new PaymentPrepareResponse(paymentRequest.getMerchant().getMerchantName(), paymentRequest.getTotalAmount(),
                paymentRequest.getId());
    }
}
