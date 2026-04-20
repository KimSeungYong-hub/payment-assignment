package com.practice.paymentassignment.exception;

public class DataIntegrityViolationException extends BusinessException {
    public DataIntegrityViolationException(String message) {
        super(message, ErrorCode.DATA_INTEGRITY_VIOLATION);
    }
}
