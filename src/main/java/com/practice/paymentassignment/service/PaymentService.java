package com.practice.paymentassignment.service;

import com.practice.paymentassignment.dto.PaymentPrepareRequest;
import com.practice.paymentassignment.dto.PaymentPrepareResponse;
import com.practice.paymentassignment.dto.PaymentRequest;
import com.practice.paymentassignment.dto.PaymentApproveResponse;
import com.practice.paymentassignment.entity.*;
import com.practice.paymentassignment.exception.*;
import com.practice.paymentassignment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;


    @Transactional
    public PaymentApproveResponse requestPayment(PaymentRequest request, Long userId) {
        log.info("Payment request received for paymentId: {}, userId: {}", request.getPaymentId(), userId);
        PaymentRequestEntity paymentRequestEntity = paymentRequestRepository.findByIdWithPessimisticLock(request.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException("주문 정보를 찾을 수 없습니다."));

        validatePaymentStatus(paymentRequestEntity);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자 정보를 찾을 수 없습니다."));
//        User user = userRepository.getReferenceById(userId);//?

        if (paymentRequestEntity.isExpired()) {
            log.warn("Payment request {} expired. Marking as EXPIRED.", paymentRequestEntity.getId());
            paymentRequestEntity.markAsExpired();
            return saveFailAndReturn(paymentRequestEntity, user, "결제 시간 만료", "결제 시간이 만료되었습니다. 처음부터 다시 시도해주세요.");
        }
        validatePaymentRequest(paymentRequestEntity, request);

        Wallet wallet = walletRepository.findByIdWithPessimisticLock(userId)
                .orElseThrow(() -> new WalletNotFoundException("사용자의 지갑 정보를 찾을 수 없습니다."));

        boolean isPaid = wallet.pay(paymentRequestEntity.getTotalAmount());

        if(!isPaid){
            log.info("Payment failed for paymentId: {} due to insufficient balance for userId: {}", request.getPaymentId(), userId);
            return saveFailAndReturn(paymentRequestEntity, user, "잔액 부족", "잔액이 부족합니다.");
        }

        Payment payment = Payment.createSuccess(paymentRequestEntity, user, paymentRequestEntity.getTotalAmount());
        paymentRepository.save(payment);

        log.info("Payment successful for paymentId: {}, userId: {}", request.getPaymentId(), userId);
        paymentRequestEntity.markAsDone();

        return PaymentApproveResponse.of(true, "결제가 완료되었습니다.");
    }

    @Transactional
    public PaymentPrepareResponse preparePayment(PaymentPrepareRequest request, String idempotencyKey) {
        log.info("Preparing payment for merchantId: {}, idempotencyKey: {}", request.getMerchantId(), idempotencyKey);
        Long merchantId = request.getMerchantId();

//        Wallet wallet = walletRepository.findByUserId(userId)
//                .orElseThrow(() -> new UserNotFoundException("사용자의 지갑 정보를 찾을 수 없습니다."));

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException("가맹점을 찾을 수 없습니다."));
        BigDecimal totalAmount = merchant.getAmount();
        PaymentRequestEntity paymentRequestEntity = PaymentRequestEntity.builder()
                .merchant(merchant)
                .totalAmount(totalAmount)
                .orderId(idempotencyKey)
                .build();

        paymentRequestRepository.save(paymentRequestEntity);
        log.info("Payment request prepared with ID: {}", paymentRequestEntity.getId());

        return PaymentPrepareResponse.from(paymentRequestEntity);
    }
    @Transactional(readOnly = true)
    public com.practice.paymentassignment.dto.PaymentInfoResponse getPaymentInfo(Long paymentId) {
        PaymentRequestEntity payment = paymentRequestRepository.findByIdWithMerchant(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("존재하지 않는 주문 번호입니다."));
        return com.practice.paymentassignment.dto.PaymentInfoResponse.from(payment);
    }


    private void validatePaymentRequest(PaymentRequestEntity paymentRequest, PaymentRequest request) {

        if (!paymentRequest.getMerchant().getId().equals(request.getMerchantId())) {
            throw new IllegalArgumentException("가맹점 정보가 일치하지 않습니다. (위변조 의심)");
        }

        if (paymentRequest.getTotalAmount().compareTo(request.getAmount()) != 0) {
            throw new IllegalArgumentException("결제 요청 금액이 실제 주문 금액과 일치하지 않습니다. (위변조 결제 방어)");
        }
    }

    public void validatePaymentStatus(PaymentRequestEntity paymentRequest) {
        if (!paymentRequest.getStatus().equals(PaymentRequestStatus.READY)) {
            throw new AlreadyProcessedException("이미 처리 중이거나 완료된 주문입니다.");
        }

    }

    private PaymentApproveResponse saveFailAndReturn(
            PaymentRequestEntity paymentRequest, User user, String reason, String message) {
        paymentRepository.save(Payment.createFail(paymentRequest, user, paymentRequest.getTotalAmount(), reason));
        return PaymentApproveResponse.of(false, message);
    }


}