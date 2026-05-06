package com.practice.paymentassignment.domain.payment.service;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.domain.payment.Payment;
import com.practice.paymentassignment.domain.payment.PaymentRequest;
import com.practice.paymentassignment.domain.payment.repository.PaymentRepository;
import com.practice.paymentassignment.domain.payment.repository.PaymentRequestRepository;
import com.practice.paymentassignment.domain.user.User;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.global.exception.ErrorCode;
import com.practice.paymentassignment.global.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentRequestRepository paymentRequestRepository;

    @Transactional
    public void recordFailure(PaymentRequest paymentRequest, User user, BigDecimal totalAmount, ErrorCode errorCode, String message){
        if(errorCode.equals(ErrorCode.PAYMENT_EXPIRED)){
            paymentRequest.markAsExpired();
        }else{
            paymentRequest.markAsFail();
        }
        paymentRepository.save(Payment.createFail(paymentRequest, user, totalAmount, message));
    }

    @Transactional
    public void recordSuccess(PaymentRequest paymentRequest, User user, BigDecimal totalAmount) {
        paymentRequest.markAsDone();
        paymentRepository.save(Payment.createSuccess(paymentRequest, user, totalAmount));
    }


    @Transactional
    public PaymentRequest findPaymentRequestWithLock(Long paymentId) {
        return paymentRequestRepository
                .findByIdWithPessimisticLock(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("주문 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public PaymentDto.Prepare.Response savePaymentRequest(Merchant merchant, String idempotencyKey) {
        log.info("Preparing payment for merchantId: {}, idempotencyKey: {}", merchant.getId(), idempotencyKey);

        // 이미 같은 멱등키로 생성된 결제 요청이 있는지 확인
        //멱등키 생성 시 어떤 방식으로 하는게 효율적인가 uuidv4 문자열 ->인덱스 단편화?
        //uuid v7 시간순 정렬 가능으로 성능 방어 탁월
        Optional<PaymentRequest> existingRequest = paymentRequestRepository.findByOrderId(idempotencyKey);

        if (existingRequest.isPresent()) {
            log.info("Existing payment request found with ID: {}", existingRequest.get().getId());
            return PaymentDto.Prepare.Response.from(existingRequest.get());
        }

        BigDecimal totalAmount = merchant.getAmount();
        PaymentRequest newRequest = PaymentRequest.createSuccess(merchant, totalAmount, idempotencyKey);
        paymentRequestRepository.save(newRequest);
        log.info("New payment request created with ID: {}", newRequest.getId());
        return PaymentDto.Prepare.Response.from(newRequest);
    }

    @Transactional(readOnly = true)
    public PaymentDto.Info.Response getPaymentInfo(Long paymentId) {
        PaymentRequest payment = paymentRequestRepository.findByIdWithMerchant(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("존재하지 않는 주문 번호입니다."));
        return PaymentDto.Info.Response.from(payment);
    }

}