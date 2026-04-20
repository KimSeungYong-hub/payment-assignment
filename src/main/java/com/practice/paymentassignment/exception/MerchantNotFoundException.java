package com.practice.paymentassignment.exception;

public class MerchantNotFoundException extends BusinessException {
    public MerchantNotFoundException() {
        super(ErrorCode.MERCHANT_NOT_FOUND);
    }
    public MerchantNotFoundException(String message) {
        super(message, ErrorCode.MERCHANT_NOT_FOUND);
    }
}
