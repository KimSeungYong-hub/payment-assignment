package com.practice.paymentassignment.service;

import com.practice.paymentassignment.domain.merchant.MerchatService;
import com.practice.paymentassignment.domain.user.UserService;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.entity.*;
import com.practice.paymentassignment.exception.*;
import com.practice.paymentassignment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final MerchatService merchatService;

    private final PaymentRepository paymentRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final WalletRepository walletRepository;
    private final UserService userService;

    @Transactional
    public PaymentDto.Approve.Response confirmPayment(PaymentDto.Approve.Request request, Long userId) {
        log.info("Payment request received for paymentId: {}, userId: {}", request.getPaymentId(), userId);

        PaymentRequestEntity paymentRequest = findPaymentRequestWithLock(request.getPaymentId());
        BigDecimal totalAmount = paymentRequest.getTotalAmount();

        User user = userService.findUser(userId);

        paymentRequest.verifyCanBeApproved(request.getPaymentId(), request.getAmount());
        if (paymentRequest.isExpired()) {
            log.warn("Payment request {} expired. Marking as EXPIRED.", paymentRequest.getId());
            paymentRequest.markAsExpired();
            paymentRepository.save(Payment.createFail(paymentRequest, user, totalAmount, "결제 시간 만료"));
            return PaymentDto.Approve.Response.of(false, "결제 시간이 만료되었습니다. 처음부터 다시 시도해주세요.");
        }

        Wallet wallet = findWalletWithLock(userId);
        boolean isPaid = wallet.pay(totalAmount);
        if (!isPaid) {
            log.info("Payment failed for paymentId: {} due to insufficient balance for userId: {}",
                    request.getPaymentId(), userId);
            paymentRepository.save(Payment.createFail(paymentRequest, user, totalAmount, "잔액 부족"));
            return PaymentDto.Approve.Response.of(false, "잔액이 부족합니다.");
        }

        Payment payment = Payment.createSuccess(paymentRequest, user, totalAmount);
        paymentRepository.save(payment);
        paymentRequest.markAsDone();
        log.info("Payment successful for paymentId: {}, userId: {}", request.getPaymentId(), userId);

        return PaymentDto.Approve.Response.of(true, "결제가 완료되었습니다.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentDto.Approve.Response recordFailure(PaymentRequestEntity paymentRequest, User user, BigDecimal totalAmount, Exception e){
        paymentRequest.markAsExpired();
        paymentRepository.save(Payment.createFail(paymentRequest, user, totalAmount, "결제 시간 만료"));
        return PaymentDto.Approve.Response.of(false, e.getMessage());
    }


    private Wallet findWalletWithLock(Long userId) {
        return walletRepository.findByUserIdWithPessimisticLock(userId)
                .orElseThrow(() -> new WalletNotFoundException("사용자의 지갑 정보를 찾을 수 없습니다."));
    }



    public PaymentRequestEntity findPaymentRequestWithLock(Long paymentId) {
        return paymentRequestRepository
                .findByIdWithPessimisticLock(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("주문 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public PaymentDto.Prepare.Response readyPayment(PaymentDto.Prepare.Request request, String idempotencyKey) {
        log.info("Preparing payment for merchantId: {}, idempotencyKey: {}", request.getMerchantId(), idempotencyKey);
        Long merchantId = request.getMerchantId();

        Merchant merchant = merchatService.findMerchant(merchantId);
        BigDecimal totalAmount = merchant.getAmount();
        PaymentRequestEntity paymentRequestEntity = PaymentRequestEntity.builder()
                .merchant(merchant)
                .totalAmount(totalAmount)
                .orderId(idempotencyKey)
                .build();

        paymentRequestRepository.save(paymentRequestEntity);
        log.info("Payment request prepared with ID: {}", paymentRequestEntity.getId());

        return PaymentDto.Prepare.Response.from(paymentRequestEntity);
    }

    @Transactional(readOnly = true)
    public PaymentDto.Info.Response getPaymentInfo(Long paymentId) {
        PaymentRequestEntity payment = paymentRequestRepository.findByIdWithMerchant(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("존재하지 않는 주문 번호입니다."));
        return PaymentDto.Info.Response.from(payment);
    }

//    private void validatePaymentRequest(PaymentRequestEntity paymentRequest, PaymentDto.Approve.Request request) {
//
//
//    }
//
//    public void validatePaymentStatus(PaymentRequestEntity paymentRequest) {
//
//    }

//    private PaymentDto.Approve.Response saveFailAndReturn(
//            PaymentRequestEntity paymentRequest, User user, String reason, String message) {
//        paymentRepository.save(Payment.createFail(paymentRequest, user, paymentRequest.getTotalAmount(), reason));
//        return PaymentDto.Approve.Response.of(false, message);
//    }

}