package com.practice.paymentassignment.domain.merchant.service;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.exception.MerchantNotFoundException;
import com.practice.paymentassignment.domain.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MerchatService {
    private final MerchantRepository merchantRepository;

    public Merchant findMerchant(Long merchantId){
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException("가맹점을 찾을 수 없습니다."));
    }
}
