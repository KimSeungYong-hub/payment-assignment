package com.practice.paymentassignment.domain.payment.service;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.domain.merchant.service.MerchatService;
import com.practice.paymentassignment.domain.payment.Payment;
import com.practice.paymentassignment.domain.payment.PaymentRequestEntity;
import com.practice.paymentassignment.domain.payment.repository.PaymentRepository;
import com.practice.paymentassignment.domain.payment.repository.PaymentRequestRepository;
import com.practice.paymentassignment.domain.user.User;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.exception.*;
import lombok.RequiredArgsConstructor;
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
//    private final WalletRepository walletRepository;
//    private final UserService userService;

//    @Transactional
//    public PaymentDto.Approve.Response confirmPayment(PaymentDto.Approve.Request request, Long userId) {
//        log.info("Payment request received for paymentId: {}, userId: {}", request.getPaymentId(), userId);
//
//        PaymentRequestEntity paymentRequest = findPaymentRequestWithLock(request.getPaymentId());
//        BigDecimal totalAmount = paymentRequest.getTotalAmount();
//
//        User user = userService.findUser(userId);
//
//        paymentRequest.verifyCanBeApproved(request.getPaymentId(), request.getAmount());
//        if (paymentRequest.isExpired()) {
//            log.warn("Payment request {} expired. Marking as EXPIRED.", paymentRequest.getId());
//            paymentRequest.markAsExpired();
//            paymentRepository.save(Payment.createFail(paymentRequest, user, totalAmount, "결제 시간 만료"));
//            return PaymentDto.Approve.Response.of(false, "결제 시간이 만료되었습니다. 처음부터 다시 시도해주세요.");
//        }
//
//        Wallet wallet = findWalletWithLock(userId);
//        boolean isPaid = wallet.pay(totalAmount);
//        if (!isPaid) {
//            log.info("Payment failed for paymentId: {} due to insufficient balance for userId: {}",
//                    request.getPaymentId(), userId);
//            paymentRepository.save(Payment.createFail(paymentRequest, user, totalAmount, "잔액 부족"));
//            return PaymentDto.Approve.Response.of(false, "잔액이 부족합니다.");
//        }
//
//        Payment payment = Payment.createSuccess(paymentRequest, user, totalAmount);
//        paymentRepository.save(payment);
//        paymentRequest.markAsDone();
//        log.info("Payment successful for paymentId: {}, userId: {}", request.getPaymentId(), userId);
//
//        return PaymentDto.Approve.Response.of(true, "결제가 완료되었습니다.");
//    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(PaymentRequestEntity paymentRequest, User user, BigDecimal totalAmount, String message){
        if(message.equals(ErrorCode.PAYMENT_EXPIRED.getMessage())){
            paymentRequest.markAsExpired();
        }else{
            paymentRequest.markAsFail();
        }
        paymentRepository.save(Payment.createFail(paymentRequest, user, totalAmount, message));
    }

    @Transactional
    public void recordSuccess(PaymentRequestEntity paymentRequest, User user, BigDecimal totalAmount) {
        paymentRequest.markAsDone();
        paymentRepository.save(Payment.createSuccess(paymentRequest, user, totalAmount));
    }

//    private Wallet findWalletWithLock(Long userId) {
//        return walletRepository.findByUserIdWithPessimisticLock(userId)
//                .orElseThrow(() -> new WalletNotFoundException("사용자의 지갑 정보를 찾을 수 없습니다."));
//    }


    @Transactional
    public PaymentRequestEntity findPaymentRequestWithLock(Long paymentId) {
        return paymentRequestRepository
                .findByIdWithPessimisticLock(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("주문 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public PaymentDto.Prepare.Response savePaymentRequest(Merchant merchant, String idempotencyKey) {
        log.info("Preparing payment for merchantId: {}, idempotencyKey: {}", merchant.getId(), idempotencyKey);

        BigDecimal totalAmount = merchant.getAmount();
        PaymentRequestEntity paymentRequestEntity = PaymentRequestEntity.createSuccess(merchant, totalAmount, idempotencyKey);
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