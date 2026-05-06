package com.practice.paymentassignment.controller;

import com.practice.paymentassignment.domain.payment.service.PaymentService;
import com.practice.paymentassignment.global.annotation.Idempotent;
import com.practice.paymentassignment.PaymentUseCase;
import com.practice.paymentassignment.dto.PaymentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/sft")
@RequiredArgsConstructor
@RestController
public class PaymentController {
    private final PaymentUseCase paymentUseCase;
    private final PaymentService paymentService;
    //TTL 설정을 어느정도로 잡아야 하나
    @Idempotent(ttl = 300, prefix = "idempotency:readyPayment:")
    @PostMapping("/ready")
    public ResponseEntity<PaymentDto.Prepare.Response> readyPayment(@RequestBody PaymentDto.Prepare.Request request,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        PaymentDto.Prepare.Response response = paymentUseCase.readyPayment(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @Idempotent(ttl = 300, prefix = "idempotency:confirmPayment:")
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
