package com.practice.paymentassignment.service;

import com.practice.paymentassignment.dto.PaymentPrepareRequest;
import com.practice.paymentassignment.dto.PaymentPrepareResponse;
import com.practice.paymentassignment.dto.PaymentRequest;
import com.practice.paymentassignment.dto.PaymentApproveResponse;
import com.practice.paymentassignment.entity.Merchant;
import com.practice.paymentassignment.entity.Payment;
import com.practice.paymentassignment.entity.PaymentStatus;
import com.practice.paymentassignment.entity.User;
import com.practice.paymentassignment.exception.MerchantNotFoundException;
import com.practice.paymentassignment.exception.UserNotFoundException;
import com.practice.paymentassignment.repository.MerchantRepository;
import com.practice.paymentassignment.repository.PaymentRepository;
import com.practice.paymentassignment.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class PaymentService {
    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;


    @Transactional
    public PaymentApproveResponse requestPayment(PaymentRequest request, String idempotencyKey){
        String merchantName = request.getMerchantName();
        Long merchantId = request.getMerchantId();
        Long userId = request.getUserId();
        int amount = request.getAmount();

        validatePaymentStatus(idempotencyKey);
        validateMerchant(merchantName);

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        user.pay(amount);

        Payment payment = paymentRepository.findByOrderId(idempotencyKey);
        payment.complete();



        return PaymentApproveResponse.from(true, "결제가 완료되었습니다.");
    }

    public void validateMerchant(String merchantName){
        if(!merchantRepository.existsByMerchantName((merchantName))){
            throw new MerchantNotFoundException("유효하지 않은 가맹점입니다.");
        }
    }

    public void validatePaymentStatus(String orderId){
        if(!paymentRepository.existsByOrderId(orderId)){
            throw new MerchantNotFoundException("이미 처리 중이거나 완료된 주문입니다.");
        }
    }


    public PaymentPrepareResponse preparePayment(PaymentPrepareRequest request) {
        Long userId = request.getUserId();
        Long merchantId = request.getMerchantId();
        int amount = request.getAmount();
        String orderId = generateOrderId(merchantId);

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        Merchant merchant = merchantRepository.findById(merchantId).orElseThrow(() -> new MerchantNotFoundException("가맹점을 찾을 수 없습니다."));

        String merchantName = merchant.getMerchantName();

        Payment payment = Payment.builder()
                .user(user)
                .merchant(merchant)
                .status(PaymentStatus.READY)
                .amount(amount)
                .orderId(orderId)
                .build();

        paymentRepository.save(payment);

        return PaymentPrepareResponse.from(merchantName, amount, orderId);

    }

    private String generateOrderId(Long merchantId) {
        String orderId = merchantId +"_ORDER_" + UUID.randomUUID();
        return orderId;
    }
}
