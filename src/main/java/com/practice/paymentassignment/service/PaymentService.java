package com.practice.paymentassignment.service;

import com.practice.paymentassignment.dto.PaymentRequest;
import com.practice.paymentassignment.entity.Merchant;
import com.practice.paymentassignment.entity.Payment;
import com.practice.paymentassignment.entity.User;
import com.practice.paymentassignment.exception.MerchantNotFoundException;
import com.practice.paymentassignment.exception.UserNotFoundException;
import com.practice.paymentassignment.repository.MerchantRepository;
import com.practice.paymentassignment.repository.PaymentRepository;
import com.practice.paymentassignment.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentService {
    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;


    @Transactional
    public boolean requestPayment(PaymentRequest request){
        String merchantName = request.getMerchantName();
        Long userId = request.getUserId();
        int amount = request.getAmount();
        validatePaymentStatus(orderId);

        validateMerchant(merchantName);
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId,"사용자를 찾을 수 없습니다."));
        user.pay(amount);


        return true;
    }

    public void validateMerchant(String merchantName){
        if(!merchantRepository.existsByMerchantName((merchantName))){
            throw new MerchantNotFoundException("유효하지 않은 가맹점입니다.");
        }
    }

    public void validatePaymentStatus(Long orderId){
        final Payment payment = paymentRepository.findById(orderId);
        if(payment.status){
            throw new MerchantNotFoundException("유효하지 않은 가맹점입니다.");
        }
    }


}
