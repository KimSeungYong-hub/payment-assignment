package com.practice.paymentassignment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentRequestStatus {

    READY("결제 준비"),
    SUCCESS("결제 성공"),
    FAILURE("결제 실패"),
    CANCELED("결제 취소");
    private final String description;

}
