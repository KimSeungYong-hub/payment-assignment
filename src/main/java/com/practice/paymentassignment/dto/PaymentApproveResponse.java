package com.practice.paymentassignment.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentApproveResponse {
    private final boolean isSuccess;
    private final String message;

    public static PaymentApproveResponse of(boolean isSuccess, String message) {
        return new PaymentApproveResponse(isSuccess, message);
    }
}
