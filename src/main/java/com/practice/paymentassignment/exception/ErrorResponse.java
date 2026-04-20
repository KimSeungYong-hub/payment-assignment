package com.practice.paymentassignment.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponse {
    private boolean isSuccess;
    private String message;
    private String code;

    private ErrorResponse(ErrorCode code) {
        this.isSuccess = false;
        this.message = code.getMessage();
        this.code = code.getCode();
    }

    private ErrorResponse(ErrorCode code, String message) {
        this.isSuccess = false;
        this.message = message;
        this.code = code.getCode();
    }

    public static ErrorResponse of(ErrorCode code) {
        return new ErrorResponse(code);
    }

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code, message);
    }
}
