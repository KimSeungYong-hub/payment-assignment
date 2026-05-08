package com.practice.paymentassignment;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.domain.merchant.service.MerchantService;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.global.exception.InsufficientBalanceException;
import com.practice.paymentassignment.global.exception.PaymentExpiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// UseCase components들이 어떠한 레이어에 위치해야하는가를 고민해보시길 바립니다.
@RequiredArgsConstructor
@Service
public class PaymentUseCase {
    private final MerchantService merchantService;
    private final PaymentTransactionProcessor paymentTransactionProcessor;

    // readyPayment는 메서드명이 명확하지 않습니다.
    // 차라리 createPayment가 나을 것 같습니다.
    @Transactional
    public PaymentDto.Prepare.Response createPayment(PaymentDto.Prepare.Request request, String idempotencyKey) {
        Merchant merchant = merchantService.findMerchant(request.getMerchantId());
        return paymentTransactionProcessor.savePaymentRequest(merchant, idempotencyKey);
    }

    //PaymentUseCase, PaymentTransactionProcessor 좋지 못한 구조? 트랜잭션 분리를 위해 PaymentTransactionProcessor 만든 상황
    //더 효율적인 방법 없을까-> 이벤트 처리 방식? 
    // Event를 사용한다면 항상 실패를 고려하여 설계하여야 합니다. Event는 silver bullet이 아닙니다.
    public PaymentDto.Approve.Response confirmPayment(PaymentDto.Approve.Request request, Long userId) {
        try{ // REQUIRES_NEW propagation를 고려해볼 것 같습니다.
            paymentTransactionProcessor.successPayment(request, userId);
            return PaymentDto.Approve.Response.of(true, "결제가 완료되었습니다.");
        }catch (InsufficientBalanceException|PaymentExpiredException e){
            // 예외 핸들링을 위해서만 존재하는 exception의 attributes를 비즈니스 로직에 침투시키면 유지보수가 힘들 것 같습니다.
            paymentTransactionProcessor.failPayment(request, userId,e.getErrorCode(), e.getMessage());
            throw e;
        }
    }

}
