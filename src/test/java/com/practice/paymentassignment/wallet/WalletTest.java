package com.practice.paymentassignment.wallet;

import com.practice.paymentassignment.entity.Wallet;
import com.practice.paymentassignment.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WalletTest {

    @Test
    @DisplayName("지갑 잔액이 결제 금액보다 적은 경우 false를 반환한다.")
    void pay_FailsDueToBalance() {
        // given
        User user = new User(1L, "Tester");
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(new BigDecimal("9000"))
                .build();

        // when
        boolean result = wallet.pay(new BigDecimal("10000"));

        // then
        assertThat(result).isFalse();
        assertThat(wallet.getBalance()).isEqualByComparingTo("9000"); // 잔액 불변 확인
    }

    @Test
    @DisplayName("결제 금액이 0원 이하인 경우 IllegalArgumentException 발생")
    void pay_FailsDueToInvalidAmount() {
        // given
        User user = new User(1L, "Tester");
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(new BigDecimal("9000"))
                .build();

        // when & then
        assertThrows(IllegalArgumentException.class, () -> wallet.pay(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> wallet.pay(new BigDecimal("-100")));
    }

    @Test
    @DisplayName("지갑 잔액이 결제 금액보다 많은 경우")
    void pay_Success(){
        // given
        User user = new User(1L, "Tester");
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(new BigDecimal("15000"))
                .build();
        // when
        wallet.pay(new BigDecimal("10000"));

        //then
        assertThat(wallet.getBalance()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("지갑 잔액이 결제 금액과 동일한 경우")
    void pay_ExactAmount(){
        // given
        User user = new User(1L, "Tester");
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(new BigDecimal("10000"))
                .build();
        // when
        wallet.pay(new BigDecimal("10000"));

        //then
        assertThat(wallet.getBalance()).isEqualByComparingTo("0");
    }
}
