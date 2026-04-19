package com.practice.paymentassignment.controller;

import com.practice.paymentassignment.dto.PaymentPrepareRequest;
import com.practice.paymentassignment.dto.PaymentPrepareResponse;
import com.practice.paymentassignment.dto.PaymentRequest;
import com.practice.paymentassignment.dto.PaymentApproveResponse;
import com.practice.paymentassignment.service.PaymentService;
import lombok.RequiredArgsConstructor;
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
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<com.practice.paymentassignment.dto.PaymentInfoResponse> getPaymentInfo(
            @PathVariable("paymentId") Long paymentId) {
        com.practice.paymentassignment.dto.PaymentInfoResponse response = paymentService.getPaymentInfo(paymentId);
        return ResponseEntity.ok(response);
    }

}
