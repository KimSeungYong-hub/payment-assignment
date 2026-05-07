package com.practice.paymentassignment.domain.wallet;

import com.practice.paymentassignment.global.exception.InsufficientBalanceException;
import com.practice.paymentassignment.global.exception.WalletNotFoundException;
import com.practice.paymentassignment.domain.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class WalletService {
    private final WalletRepository walletRepository;

    @Transactional
    public void deduct(Long userId, BigDecimal amount) {
        Wallet wallet = findWalletWithLock(userId);
        BigDecimal beforeBalance = wallet.getBalance();
        log.info("Before deduct - userId: {}, currentBalance: {}, amount: {}", userId, beforeBalance, amount);
        wallet.pay(amount);
        BigDecimal afterBalance = wallet.getBalance();
        log.info("After deduct - userId: {}, newBalance: {}, deducted: {}", userId, afterBalance, amount);
    }

    private Wallet findWalletWithLock(Long userId) {
        return walletRepository.findByUserIdWithPessimisticLock(userId)
                .orElseThrow(() -> new WalletNotFoundException("사용자의 지갑 정보를 찾을 수 없습니다."));
    }

}
