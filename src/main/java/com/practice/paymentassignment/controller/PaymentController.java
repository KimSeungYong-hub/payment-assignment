package com.practice.paymentassignment.controller;

import com.practice.paymentassignment.dto.PaymentPrepareRequest;
import com.practice.paymentassignment.dto.PaymentPrepareResponse;
import com.practice.paymentassignment.dto.PaymentRequest;
import com.practice.paymentassignment.dto.PaymentApproveResponse;
import com.practice.paymentassignment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/sft")
@RequiredArgsConstructor
@RestController
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/prepare")
    public ResponseEntity<PaymentPrepareResponse> preparePayment(@RequestBody PaymentPrepareRequest request,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        PaymentPrepareResponse response = paymentService.preparePayment(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/approve")
    public ResponseEntity<PaymentApproveResponse> approvePayment(@RequestBody PaymentRequest request) {
        PaymentApproveResponse response = paymentService.requestPayment(request);

        if (!response.isSuccess()) {
            // 비즈니스 실패 (잔액 부족 등) -> HTTP 400 상태 코드로 반환!
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<com.practice.paymentassignment.dto.PaymentInfoResponse> getPaymentInfo(
            @PathVariable("paymentId") Long paymentId) {
        com.practice.paymentassignment.dto.PaymentInfoResponse response = paymentService.getPaymentInfo(paymentId);
        return ResponseEntity.ok(response);
    }

}
