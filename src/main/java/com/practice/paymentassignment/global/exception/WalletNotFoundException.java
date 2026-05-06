package com.practice.paymentassignment.global.exception;

public class WalletNotFoundException extends BusinessException {
    public WalletNotFoundException(String message) {
        super(message, ErrorCode.WALLET_NOT_FOUND);
    }
    public WalletNotFoundException() {
        super(ErrorCode.WALLET_NOT_FOUND);
    }

}
