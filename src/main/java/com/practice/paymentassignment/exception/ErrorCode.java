package com.practice.paymentassignment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid input value"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "Internal server error"),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "C003", "Data integrity violation"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found"),

    // Merchant
    MERCHANT_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "Merchant not found"),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "Payment not found"),

    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "W001", "Wallet not found");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
