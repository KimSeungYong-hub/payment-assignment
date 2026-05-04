package com.practice.paymentassignment.wallet;

import com.practice.paymentassignment.domain.user.User;
import com.practice.paymentassignment.domain.wallet.Wallet;
import com.practice.paymentassignment.domain.wallet.WalletService;
import com.practice.paymentassignment.domain.wallet.repository.WalletRepository;
import com.practice.paymentassignment.exception.InsufficientBalanceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    @Test
    @DisplayName("사용자 지갑 잔액 부족 시 InsufficientBalanceException예외 발생")
    void wallet_FailsDueToInsufficientBalance(){
        // given
        User user = new User(1L, "Tester");
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(new BigDecimal("5000"))
                .build();
        given(walletRepository.findByUserIdWithPessimisticLock(user.getId())).willReturn(Optional.ofNullable(wallet));
        // when & then
        assertThrows(InsufficientBalanceException.class, () -> walletService.deduct(user.getId(), new BigDecimal("10000")));
    }
}
