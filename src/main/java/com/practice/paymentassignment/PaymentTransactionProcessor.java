package com.practice.paymentassignment;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.domain.payment.PaymentRequest;
import com.practice.paymentassignment.domain.payment.service.PaymentService;
import com.practice.paymentassignment.domain.user.User;
import com.practice.paymentassignment.domain.user.UserService;
import com.practice.paymentassignment.domain.wallet.WalletService;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentTransactionProcessor {
    private final PaymentService paymentService;
    private final UserService userService;
    private final WalletService walletService;

    @Transactional
    public void successPayment(PaymentDto.Approve.Request request, Long userId){
        PaymentRequest paymentRequest = paymentService.findPaymentRequestWithLock(request.getPaymentId());

        User user = userService.findUser(userId);
        paymentRequest.verifyCanBeApproved(request.getMerchantId(), request.getAmount());

        BigDecimal totalAmount = paymentRequest.getTotalAmount();
        walletService.deduct(userId, totalAmount);

        paymentService.recordSuccess(paymentRequest, user, totalAmount);
    }

    @Transactional
    public void failPayment(PaymentDto.Approve.Request request, Long userId, ErrorCode errorCode, String message) {
        PaymentRequest paymentRequest = paymentService.findPaymentRequestWithLock(request.getPaymentId());

        User user = userService.findUser(userId);

        BigDecimal totalAmount = paymentRequest.getTotalAmount();
        paymentService.recordFailure(paymentRequest, user, totalAmount, errorCode, message);
    }

    @Transactional
    public PaymentDto.Prepare.Response savePaymentRequest(Merchant merchant, String idempotencyKey) {
        return paymentService.savePaymentRequest(merchant, idempotencyKey);
    }

}
