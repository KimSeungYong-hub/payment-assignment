package com.practice.paymentassignment.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class PaymentPrepareRequest {
    private final Long userId;
    private final Long merchantId;
    private final BigDecimal amount;

}
