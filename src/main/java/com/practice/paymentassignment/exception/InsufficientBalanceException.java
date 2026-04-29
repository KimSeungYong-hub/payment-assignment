package com.practice.paymentassignment.exception;

public class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(String message) {
        super(message, ErrorCode.INSUFFICIENT_BALANCE);
    }
}
