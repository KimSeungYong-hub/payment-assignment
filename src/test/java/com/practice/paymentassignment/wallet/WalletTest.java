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
    @DisplayName("지갑 잔액이 결제 금액보다 적은 경우 예외 발생")
     void pay_Fali(){
        // given
        User user = new User(1L, "Tester");
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(new BigDecimal("9000"))
                .build();

        // when & then
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> wallet.pay(new BigDecimal("10000"))
        );
        assertThat(exception.getMessage()).contains("잔액이 부족합니다");
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
