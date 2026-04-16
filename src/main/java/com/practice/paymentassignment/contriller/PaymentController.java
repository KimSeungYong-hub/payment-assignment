package com.practice.paymentassignment.contriller;

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
    public ResponseEntity<PaymentPrepareResponse> preparePayment(@RequestBody PaymentPrepareRequest request){
        PaymentPrepareResponse response = paymentService.preparePayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/approve")
    public ResponseEntity<PaymentApproveResponse> approvePayment(@RequestBody PaymentRequest request,
                                                                 @RequestHeader(value = "Idempotency-Key") String idempotencyKey)
    {
        PaymentApproveResponse response = paymentService.requestPayment(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }
}
