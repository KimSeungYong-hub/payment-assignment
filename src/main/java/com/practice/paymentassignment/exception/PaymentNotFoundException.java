package com.practice.paymentassignment.exception;

public class PaymentNotFoundException extends BusinessException {
    public PaymentNotFoundException() {
        super(ErrorCode.PAYMENT_NOT_FOUND);
    }
    public PaymentNotFoundException(String message) {
        super(message, ErrorCode.PAYMENT_NOT_FOUND);
    }
}
