package com.practice.paymentassignment;

import com.practice.paymentassignment.domain.merchant.MerchatService;
import com.practice.paymentassignment.domain.user.UserService;
import com.practice.paymentassignment.domain.wallet.WalletService;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.entity.Payment;
import com.practice.paymentassignment.entity.PaymentRequestEntity;
import com.practice.paymentassignment.entity.User;
import com.practice.paymentassignment.entity.Wallet;
import com.practice.paymentassignment.exception.InsufficientBalanceException;
import com.practice.paymentassignment.exception.PaymentExpiredException;
import com.practice.paymentassignment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PaymentUseCase {
    private final PaymentService paymentService;
    private final UserService userService;
    private final MerchatService merchatService;
    private final WalletService walletService;

    @Transactional
    public PaymentDto.Prepare.Response readyPayment(PaymentDto.Prepare.Request request, String idempotencyKey) {


    }



    public PaymentDto.Approve.Response confirmPayment(PaymentDto.Approve.Request request, Long userId) {
        PaymentRequestEntity paymentRequest = paymentService.findPaymentRequestWithLock(request.getPaymentId());
        BigDecimal totalAmount = paymentRequest.getTotalAmount();
        try{
            paymentRequest.verifyCanBeApproved(request.getPaymentId(), request.getAmount());
            walletService.deduct(userId, request.getAmount());
        }catch (InsufficientBalanceException|PaymentExpiredException e){
            User user = userService.findUser(userId);
            paymentService.recordFailure(paymentRequest, user, totalAmount, e);
        }
        return PaymentDto.Approve.Response.of(true, "결제가 완료되었습니다.");
    }

}
