package com.practice.paymentassignment.exception;

public class UserNotFoundException extends RuntimeException {


    public UserNotFoundException(Long userId, String message) {
        super(userId+" "+ message);
    }

}
