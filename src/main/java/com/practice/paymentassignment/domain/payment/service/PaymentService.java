package com.practice.paymentassignment.domain.payment.service;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.domain.payment.Payment;
import com.practice.paymentassignment.domain.payment.PaymentRequestEntity;
import com.practice.paymentassignment.domain.payment.repository.PaymentRepository;
import com.practice.paymentassignment.domain.payment.repository.PaymentRequestRepository;
import com.practice.paymentassignment.domain.user.User;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentRequestRepository paymentRequestRepository;

    @Transactional
    public void recordFailure(PaymentRequestEntity paymentRequest, User user, BigDecimal totalAmount,ErrorCode errorCode, String message){
        if(errorCode.equals(ErrorCode.PAYMENT_EXPIRED)){
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

}