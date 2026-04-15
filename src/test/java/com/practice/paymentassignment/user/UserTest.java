package com.practice.paymentassignment.user;

import com.practice.paymentassignment.dto.PaymentRequest;
import com.practice.paymentassignment.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserTest {
    private User user;

    @Test
    @DisplayName("사용자 money가 결제 금액보다 적은 경우 예외 발생")
     void pay_Fali(){
        // given
        User user = User.builder()
                .money(9000)
                .build();

        // when & then
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> user.pay(10000)
        );
        assertThat(exception.getMessage()).contains("잔액이 부족합니다");
    }

    @Test
    @DisplayName("사용자 money가 결제 금액보다 많은 경우")
    void pay_Success(){
        // given
        User user = User.builder()
                .money(15000)
                .build();
        // when
        user.pay(10000);

        //then
        assertThat(user.getMoney()).isEqualTo(5000);
    }

    @Test
    @DisplayName("사용자 money가 결제 금액과 동일한 경우")
    void pay_ExactAmount(){
        // given
        User user = User.builder()
                .money(10000)
                .build();
        // when
        user.pay(10000);

        //then
        assertThat(user.getMoney()).isEqualTo(0);
    }
}
