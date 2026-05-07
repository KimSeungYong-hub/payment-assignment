package com.practice.paymentassignment.controller;

import com.practice.paymentassignment.domain.payment.service.PaymentService;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.global.annotation.Idempotent;
import com.practice.paymentassignment.PaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 명확하지 않습니다.
@RequestMapping("/sft")
@RequiredArgsConstructor
@RestController
public class PaymentController {
    private final PaymentUseCase paymentUseCase;
    private final PaymentService paymentService;
    //TTL 설정을 어느정도로 잡아야 하나
    @Idempotent(ttl = 300, prefix = "idempotency:readyPayment:")
    // ready는 동사 즉 행위입니다. REST API에서 행위는 오직 HTTP 메서드로 나타냅니다. resource(명사)로 나타내주세요.
    // 또한 ready라는 엔드포인트는 명확하지 않습니다. 
    // 사용자로 하여금 엔드포인트만 보고도 어떠한 역할인지 명확히 알 수 있도록 해야합니다.
    @PostMapping("/ready")
    public ResponseEntity<PaymentDto.Prepare.Response> readyPayment(@RequestBody PaymentDto.Prepare.Request request,
                                                                    @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        PaymentDto.Prepare.Response response = paymentUseCase.readyPayment(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @Idempotent(ttl = 300, prefix = "idempotency:confirmPayment:")
    @PostMapping("/confirm") // 마찬가지
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
