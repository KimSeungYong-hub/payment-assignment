package com.practice.paymentassignment;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.domain.payment.PaymentRequest;
import com.practice.paymentassignment.domain.payment.PaymentRequestStatus;
import com.practice.paymentassignment.domain.payment.service.PaymentService;
import com.practice.paymentassignment.domain.user.User;
import com.practice.paymentassignment.domain.user.UserService;
import com.practice.paymentassignment.domain.wallet.WalletService;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
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
        if (paymentRequest.getStatus().equals(PaymentRequestStatus.SUCCESS)) {
            log.info("Payment already completed - paymentId: {}", request.getPaymentId());
            return;
        }

        User user = userService.findUser(userId);
        paymentRequest.verifyCanBeApproved(request.getMerchantId(), request.getAmount());

        BigDecimal totalAmount = paymentRequest.getTotalAmount();
        walletService.deduct(userId, totalAmount);

        paymentService.recordSuccess(paymentRequest, user, totalAmount);
        log.info("Payment success completed - paymentId: {}, userId: {}, amount: {}",
            request.getPaymentId(), userId, totalAmount);
    }

    @Transactional
    public void failPayment(PaymentDto.Approve.Request request, Long userId, ErrorCode errorCode, String message) {
        PaymentRequest paymentRequest = paymentService.findPaymentRequestWithLock(request.getPaymentId());

        User user = userService.findUser(userId);

        BigDecimal totalAmount = paymentRequest.getTotalAmount();
        paymentService.recordFailure(paymentRequest, user, totalAmount, errorCode, message);

        log.error("Payment failure recorded - paymentId: {}, userId: {}, amount: {}, errorCode: {}",
            request.getPaymentId(), userId, totalAmount, errorCode);
    }

    @Transactional
    public PaymentDto.Prepare.Response savePaymentRequest(Merchant merchant, String idempotencyKey) {
        return paymentService.savePaymentRequest(merchant, idempotencyKey);
    }

}
