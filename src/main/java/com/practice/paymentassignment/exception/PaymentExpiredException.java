package com.practice.paymentassignment.exception;

public class PaymentExpiredException extends BusinessException {
    public PaymentExpiredException(String message) {
        super(message, ErrorCode.PAYMENT_EXPIRED);
    }
}
