package com.practice.paymentassignment.exception;

public class PaymentForgeryException extends BusinessException {
    public PaymentForgeryException() {
        super(ErrorCode.PAYMENT_FORGERY);
    }

    public PaymentForgeryException(String message) {
        super(message, ErrorCode.PAYMENT_FORGERY);
    }
}
