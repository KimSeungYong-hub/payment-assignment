package com.practice.paymentassignment.exception;

public class AlreadyProcessedException extends BusinessException {
    public AlreadyProcessedException(String message) {
        super(message, ErrorCode.DATA_INTEGRITY_VIOLATION);
    }
}
