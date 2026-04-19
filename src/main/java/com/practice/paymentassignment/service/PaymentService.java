package com.practice.paymentassignment.service;

import com.practice.paymentassignment.dto.PaymentPrepareRequest;
import com.practice.paymentassignment.dto.PaymentPrepareResponse;
import com.practice.paymentassignment.dto.PaymentRequest;
import com.practice.paymentassignment.dto.PaymentApproveResponse;
import com.practice.paymentassignment.entity.Merchant;
import com.practice.paymentassignment.entity.Payment;
import com.practice.paymentassignment.entity.PaymentStatus;
import com.practice.paymentassignment.exception.MerchantNotFoundException;
import com.practice.paymentassignment.exception.UserNotFoundException;
import com.practice.paymentassignment.repository.MerchantRepository;
import com.practice.paymentassignment.repository.PaymentRepository;
import com.practice.paymentassignment.repository.WalletRepository;
import com.practice.paymentassignment.entity.Wallet;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.practice.paymentassignment.exception.PaymentNotFoundException;

@RequiredArgsConstructor
@Service
public class PaymentService {
    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;

    @Transactional
    public PaymentApproveResponse requestPayment(PaymentRequest request) {
        Payment payment = paymentRepository.findByIdWithPessimisticLock(request.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException("주문 정보를 찾을 수 없습니다."));

        validatePaymentStatus(payment);
        validatePaymentRequest(payment, request);

        Wallet wallet = walletRepository.findByIdWithPessimisticLock(payment.getWallet().getId())
                .orElseThrow(() -> new IllegalArgumentException("사용자의 지갑 정보를 찾을 수 없습니다."));

        wallet.pay(payment.getAmount());
        payment.complete();

        return PaymentApproveResponse.from(true, "결제가 완료되었습니다.");
    }

    private void validatePaymentRequest(Payment payment, PaymentRequest request) {
        if (!payment.getWallet().getUser().getId().equals(request.getUserId())) {
            throw new IllegalArgumentException("결제 요청자와 주문자가 일치하지 않습니다.");
        }

        if (!payment.getMerchant().getId().equals(request.getMerchantId())) {
            throw new IllegalArgumentException("가맹점 정보가 일치하지 않습니다. (위변조 의심)");
        }

        if (payment.getAmount().compareTo(request.getAmount()) != 0) {
            throw new IllegalArgumentException("결제 요청 금액이 실제 주문 금액과 일치하지 않습니다. (위변조 결제 방어)");
        }
    }

    public void validatePaymentStatus(Payment payment) {
        if (!payment.getStatus().equals(PaymentStatus.READY)) {
            throw new PaymentNotFoundException("이미 처리 중이거나 완료된 주문입니다.");
        }
    }

    public PaymentPrepareResponse preparePayment(PaymentPrepareRequest request, String idempotencyKey) {
        Long userId = request.getUserId();
        Long merchantId = request.getMerchantId();
        java.math.BigDecimal amount = request.getAmount();

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자의 지갑 정보를 찾을 수 없습니다."));
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException("가맹점을 찾을 수 없습니다."));

        Payment payment = Payment.builder()
                .wallet(wallet)
                .merchant(merchant)
                .status(PaymentStatus.READY)
                .amount(amount)
                .orderId(idempotencyKey)
                .build();

        paymentRepository.save(payment);

        return PaymentPrepareResponse.from(payment);

    }

    public com.practice.paymentassignment.dto.PaymentInfoResponse getPaymentInfo(Long paymentId) {
        Payment payment = paymentRepository.findByIdWithMerchant(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("존재하지 않는 주문 번호입니다."));
        return com.practice.paymentassignment.dto.PaymentInfoResponse.from(payment);
    }
}