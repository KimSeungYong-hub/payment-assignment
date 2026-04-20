package com.practice.paymentassignment.exception;

import com.practice.paymentassignment.dto.PaymentApproveResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({
            UserNotFoundException.class,
            MerchantNotFoundException.class,
            PaymentNotFoundException.class,
            DataIntegrityViolationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<PaymentApproveResponse> handleBusinessExceptions(RuntimeException e) {
        return ResponseEntity.badRequest().body(new PaymentApproveResponse(false, e.getMessage()));
    }

//    @ExceptionHandler(DataIntegrityViolationException.class)
//    public ResponseEntity<PaymentApproveResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
//        return ResponseEntity.badRequest().body(new PaymentApproveResponse(false, e.getMessage()));
//    }
}
