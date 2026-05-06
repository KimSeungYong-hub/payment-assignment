package com.practice.paymentassignment;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.domain.merchant.service.MerchantService;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.global.exception.InsufficientBalanceException;
import com.practice.paymentassignment.global.exception.PaymentExpiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PaymentUseCase {
    private final MerchantService merchantService;
    private final PaymentTransactionProcessor paymentTransactionProcessor;

    @Transactional
    public PaymentDto.Prepare.Response readyPayment(PaymentDto.Prepare.Request request, String idempotencyKey) {
        Merchant merchant = merchantService.findMerchant(request.getMerchantId());
        return paymentTransactionProcessor.savePaymentRequest(merchant, idempotencyKey);
    }

    //PaymentUseCase, PaymentTransactionProcessor 좋지 못한 구조? 트랜잭션 분리를 위해 PaymentTransactionProcessor 만든 상황
    //더 효율적인 방법 없을까-> 이벤트 처리 방식?
    public PaymentDto.Approve.Response confirmPayment(PaymentDto.Approve.Request request, Long userId) {
        try{
            paymentTransactionProcessor.successPayment(request, userId);
            return PaymentDto.Approve.Response.of(true, "결제가 완료되었습니다.");
        }catch (InsufficientBalanceException|PaymentExpiredException e){
            paymentTransactionProcessor.failPayment(request, userId,e.getErrorCode(), e.getMessage());
            throw e;
        }
    }

}
