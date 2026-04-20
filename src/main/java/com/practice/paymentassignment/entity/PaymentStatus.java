package com.practice.paymentassignment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    SUCCESS("승인 성공"),
    FAIL("승인 실패");

    private final String description;
}
