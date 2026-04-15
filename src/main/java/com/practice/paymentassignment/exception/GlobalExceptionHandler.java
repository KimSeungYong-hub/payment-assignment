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
            IllegalArgumentException.class
    })
    public ResponseEntity<PaymentApproveResponse> handleBusinessExceptions(RuntimeException e) {
        return ResponseEntity.badRequest().body(new PaymentApproveResponse(false, e.getMessage()));
    }

//    @ExceptionHandler(DataIntegrityViolationException.class)
//    public ResponseEntity<PaymentResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
//        return ResponseEntity.badRequest().body(new PaymentResponse(false, "이미 처리 중이거나 완료된 주문입니다. (중복 요청)"));
//    }
}
