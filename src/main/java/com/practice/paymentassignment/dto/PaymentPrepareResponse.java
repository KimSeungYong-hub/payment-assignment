package com.practice.paymentassignment.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentPrepareResponse {
    private final String merchatName;
    private final int amount;

    private final String orderId;

    public static PaymentPrepareResponse from(String merchantName, int amount, String orderId) {
        return new PaymentPrepareResponse(merchantName, amount, orderId);
    }
}
