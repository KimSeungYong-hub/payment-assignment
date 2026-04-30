package com.practice.paymentassignment.controller;

import com.practice.paymentassignment.PaymentUseCase;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/sft")
@RequiredArgsConstructor
@RestController
public class PaymentController {
    private final PaymentUseCase paymentUseCase;
    private final PaymentService paymentService;

    @PostMapping("/ready")
    public ResponseEntity<PaymentDto.Prepare.Response> readyPayment(@RequestBody PaymentDto.Prepare.Request request,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        PaymentDto.Prepare.Response response = paymentUseCase.readyPayment(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentDto.Approve.Response> confirmPayment(@RequestBody PaymentDto.Approve.Request request) {
        Long userId = 1L;
        PaymentDto.Approve.Response response = paymentUseCase.confirmPayment(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentDto.Info.Response> getPaymentInfo(
            @PathVariable("paymentId") Long paymentId) {
        PaymentDto.Info.Response response = paymentService.getPaymentInfo(paymentId);
        return ResponseEntity.ok(response);
    }

}
