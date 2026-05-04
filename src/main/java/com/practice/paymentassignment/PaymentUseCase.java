package com.practice.paymentassignment;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.domain.merchant.service.MerchantService;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.exception.InsufficientBalanceException;
import com.practice.paymentassignment.exception.PaymentExpiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PaymentUseCase {
    private final MerchantService merchantService;
    private final PaymentTransactionProcessor paymentTransactionProcessor;

//    private final UserService userService;
//    private final WalletService walletService;


    @Transactional
    public PaymentDto.Prepare.Response readyPayment(PaymentDto.Prepare.Request request, String idempotencyKey) {
        Merchant merchant = merchantService.findMerchant(request.getMerchantId());
        return paymentTransactionProcessor.savePaymentRequest(merchant, idempotencyKey);
    }

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
