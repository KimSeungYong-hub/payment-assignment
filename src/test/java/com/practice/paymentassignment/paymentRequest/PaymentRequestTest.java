package com.practice.paymentassignment.paymentRequest;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.domain.payment.PaymentRequestEntity;
import com.practice.paymentassignment.domain.payment.PaymentRequestStatus;
import com.practice.paymentassignment.domain.user.User;
import com.practice.paymentassignment.domain.wallet.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class PaymentRequestTest {
    @Test
    @DisplayName("이미 처리 중이거나 완료된 주문인 경우 AlreadyProcessedException 발생")
    void paymentRequest_FailsDueToAlreadyProcessed() {
        // given
        Merchant merchant = Merchant.builder()
                .merchantName("test_merchant")
                .build();
        ReflectionTestUtils.setField(merchant, "id", 1L);

        PaymentRequestEntity paymentRequest = PaymentRequestEntity.builder()
                .merchant(merchant)
                .orderId("test_order_Key")
                .totalAmount(new BigDecimal("10000"))
                .build();
        ReflectionTestUtils.setField(paymentRequest, "status", PaymentRequestStatus.SUCCESS);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> paymentRequest.verifyCanBeApproved(merchant.getId(), new BigDecimal("10000")));

    }


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


}


