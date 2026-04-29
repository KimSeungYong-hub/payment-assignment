package com.practice.paymentassignment.domain.wallet;

import com.practice.paymentassignment.entity.Wallet;
import com.practice.paymentassignment.exception.InsufficientBalanceException;
import com.practice.paymentassignment.exception.WalletNotFoundException;
import com.practice.paymentassignment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class WalletService {
    private final WalletRepository walletRepository;

    @Transactional
    public void deduct(Long userId, BigDecimal amount) {
        Wallet wallet = findWalletWithLock(userId);

        boolean isPaid = wallet.pay(amount);
        if (!isPaid) {
            throw new InsufficientBalanceException("잔액이 부족합니다.");
        }
    }

    private Wallet findWalletWithLock(Long userId) {
        return walletRepository.findByUserIdWithPessimisticLock(userId)
                .orElseThrow(() -> new WalletNotFoundException("사용자의 지갑 정보를 찾을 수 없습니다."));
    }

}
