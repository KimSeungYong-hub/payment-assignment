package com.practice.paymentassignment.service;

import com.practice.paymentassignment.dto.PaymentPrepareRequest;
import com.practice.paymentassignment.dto.PaymentPrepareResponse;
import com.practice.paymentassignment.dto.PaymentRequest;
import com.practice.paymentassignment.dto.PaymentApproveResponse;
import com.practice.paymentassignment.entity.*;
import com.practice.paymentassignment.exception.DataIntegrityViolationException;
import com.practice.paymentassignment.exception.MerchantNotFoundException;
import com.practice.paymentassignment.exception.UserNotFoundException;
import com.practice.paymentassignment.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.practice.paymentassignment.exception.PaymentNotFoundException;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
public class PaymentService {
    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;


    @Transactional
    public PaymentApproveResponse requestPayment(PaymentRequest request) {
        PaymentRequestEntity paymentRequestEntity = paymentRequestRepository.findByIdWithPessimisticLock(request.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException("주문 정보를 찾을 수 없습니다."));
        Long userId = request.getUserId();

        validatePaymentStatus(paymentRequestEntity);
        validatePaymentRequest(paymentRequestEntity, request);

        Wallet wallet = walletRepository.findByIdWithPessimisticLock(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자의 지갑 정보를 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자 정보를 찾을 수 없습니다."));

        boolean isPaid = wallet.pay(paymentRequestEntity.getTotalAmount());

        if(!isPaid){
            Payment failPayment = Payment.createFail(paymentRequestEntity, user, paymentRequestEntity.getTotalAmount(), "잔액 부족");
            paymentRepository.save(failPayment);
            return PaymentApproveResponse.of(false, "잔액이 부족합니다."); // 정상 커밋 (실패 영수증 저장됨)
        }

        Payment payment = Payment.createSuccess(paymentRequestEntity, user, paymentRequestEntity.getTotalAmount());
        paymentRepository.save(payment);

        paymentRequestEntity.markAsDone();

        return PaymentApproveResponse.of(true, "결제가 완료되었습니다.");
    }

    private void validatePaymentRequest(PaymentRequestEntity paymentRequest, PaymentRequest request) {
//        if (!payment.getWallet().getUser().getId().equals(request.getUserId())) {
//            throw new IllegalArgumentException("결제 요청자와 주문자가 일치하지 않습니다.");
//        }

        if (!paymentRequest.getMerchant().getId().equals(request.getMerchantId())) {
            throw new IllegalArgumentException("가맹점 정보가 일치하지 않습니다. (위변조 의심)");
        }

        if (paymentRequest.getTotalAmount().compareTo(request.getAmount()) != 0) {
            throw new IllegalArgumentException("결제 요청 금액이 실제 주문 금액과 일치하지 않습니다. (위변조 결제 방어)");
        }
    }

    public void validatePaymentStatus(PaymentRequestEntity paymentRequest) {
        if (!paymentRequest.getStatus().equals(PaymentRequestStatus.READY)) {
            throw new DataIntegrityViolationException("이미 처리 중이거나 완료된 주문입니다.");
        }



    }

    @Transactional
    public PaymentPrepareResponse preparePayment(PaymentPrepareRequest request, String idempotencyKey) {
        Long merchantId = request.getMerchantId();

//        Wallet wallet = walletRepository.findByUserId(userId)
//                .orElseThrow(() -> new UserNotFoundException("사용자의 지갑 정보를 찾을 수 없습니다."));

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException("가맹점을 찾을 수 없습니다."));

        BigDecimal amount = merchant.getAmount();

        PaymentRequestEntity paymentRequestEntity = PaymentRequestEntity.builder()
                .merchant(merchant)
                .totalAmount(amount)
                .orderId(idempotencyKey)
                .build();

        paymentRequestRepository.save(paymentRequestEntity);

        return PaymentPrepareResponse.from(paymentRequestEntity);

    }

    public com.practice.paymentassignment.dto.PaymentInfoResponse getPaymentInfo(Long paymentId) {
        PaymentRequestEntity payment = paymentRequestRepository.findByIdWithMerchant(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("존재하지 않는 주문 번호입니다."));
        return com.practice.paymentassignment.dto.PaymentInfoResponse.from(payment);
    }
}