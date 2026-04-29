package com.practice.paymentassignment.controller;

import com.practice.paymentassignment.PaymentUseCase;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/sft")
@RequiredArgsConstructor
@RestController
public class PaymentController {
    private final PaymentUseCase paymentUseCase;

    @PostMapping("/ready")
    public ResponseEntity<PaymentDto.Prepare.Response> readyPayment(@RequestBody PaymentDto.Prepare.Request request,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        PaymentDto.Prepare.Response response = paymentService.readyPayment(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentDto.Approve.Response> confirmPayment(@RequestBody PaymentDto.Approve.Request request) {
        Long userId = 1L;
        PaymentDto.Approve.Response response = paymentUseCase.confirmPayment(request, userId);

        if (!response.isSuccess()) {
            // 비즈니스 실패 (잔액 부족 등) -> HTTP 400 상태 코드로 반환
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentDto.Info.Response> getPaymentInfo(
            @PathVariable("paymentId") Long paymentId) {
        PaymentDto.Info.Response response = paymentService.getPaymentInfo(paymentId);
        return ResponseEntity.ok(response);
    }

}
