package com.practice.paymentassignment.global.exception;

public class AlreadyProcessedException extends BusinessException {
    public AlreadyProcessedException(String message) {
        super(message, ErrorCode.DATA_INTEGRITY_VIOLATION);
    }
}
