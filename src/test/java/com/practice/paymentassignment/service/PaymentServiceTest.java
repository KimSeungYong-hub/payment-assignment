package com.practice.paymentassignment.service;

import com.practice.paymentassignment.dto.PaymentRequest;
import com.practice.paymentassignment.entity.Merchant;
import com.practice.paymentassignment.entity.Payment;
import com.practice.paymentassignment.entity.PaymentStatus;
import com.practice.paymentassignment.entity.Wallet;
import com.practice.paymentassignment.entity.User;
import com.practice.paymentassignment.repository.MerchantRepository;
import com.practice.paymentassignment.repository.PaymentRepository;
import com.practice.paymentassignment.repository.UserRepository;
import com.practice.paymentassignment.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("결제가 성공하면 유저 잔액이 깎이고 결제 내역이 저장되어야 한다.")
    void requestPayment_Success(){
        // given
        PaymentRequest request = new PaymentRequest(1L, 1L, 1L, new BigDecimal("10000"));
        User user = new User(1L, "Tester");
        Wallet wallet = Wallet.builder()
                .id(1L)
                .user(user)
                .balance(new BigDecimal("15000"))
                .build();
        Merchant merchant = Merchant.builder()
                .id(1L)
                .merchantName("test_merchant")
                .build();
        Payment payment = Payment.builder()
                .wallet(wallet)
                .amount(new BigDecimal("10000"))
                .status(PaymentStatus.READY)
                .merchant(merchant)
                .build();

        given(paymentRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(payment));
        given(walletRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(wallet));

        // when
        paymentService.requestPayment(request);

        // then
        assertThat(wallet.getBalance()).isEqualByComparingTo("5000");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("결제 요청자와 주문자가 다를 경우 예외 발생")
    void requestPayment_Fail_InvalidUser(){
        // given
        PaymentRequest request = new PaymentRequest(999L, 1L, 1L, new BigDecimal("10000")); // user 999
        User correctUser = new User(1L, "Tester");
        Wallet correctWallet = Wallet.builder().id(1L).user(correctUser).balance(new BigDecimal("10000")).build();
        Merchant merchant = Merchant.builder().id(1L).build();
        Payment payment = Payment.builder()
                .wallet(correctWallet)
                .merchant(merchant)
                .amount(new BigDecimal("10000"))
                .status(PaymentStatus.READY)
                .build();

        given(paymentRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(payment));

        // when & then
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.requestPayment(request)
        );
        assertThat(exception.getMessage()).contains("결제 요청자와 주문자가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("결제 요청 금액이 원장 금액과 다를 경우 (위변조 시도) 예외 발생")
    void requestPayment_Fail_InvalidAmount(){
        // given
        PaymentRequest request = new PaymentRequest(1L, 1L, 1L, new BigDecimal("1")); // 1원으로 조작
        User user = new User(1L, "Tester");
        Wallet wallet = Wallet.builder().id(1L).user(user).balance(new BigDecimal("10000")).build();
        Merchant merchant = Merchant.builder().id(1L).build();
        Payment payment = Payment.builder()
                .wallet(wallet)
                .merchant(merchant)
                .amount(new BigDecimal("10000")) // 진짜 원장 가격은 10000원
                .status(PaymentStatus.READY)
                .build();

        given(paymentRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(payment));

        // when & then
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.requestPayment(request)
        );
        assertThat(exception.getMessage()).contains("결제 요청 금액이 실제 주문 금액과 일치하지 않습니다");
    }

    @Test
    @DisplayName("유저 잔액이 부족할 경우 차감 시 예외 발생")
    void requestPayment_Fail_InsufficientBalance(){
        // given
        PaymentRequest request = new PaymentRequest(1L, 1L, 1L, new BigDecimal("10000"));
        User user = new User(1L, "Tester");
        Wallet wallet = Wallet.builder()
                .id(1L)
                .user(user)
                .balance(new BigDecimal("5000")) // 잔고 5000원 뿐
                .build();
        Merchant merchant = Merchant.builder().id(1L).build();
        Payment payment = Payment.builder()
                .wallet(wallet)
                .merchant(merchant)
                .amount(new BigDecimal("10000"))
                .status(PaymentStatus.READY)
                .build();

        given(paymentRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(payment));
        given(walletRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(wallet));

        // when & then
        final RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.requestPayment(request)
        );
        assertThat(exception.getMessage()).contains("잔액이 부족");
    }

    @Test
    @DisplayName("이미 처리된 결제(SUCCESS)를 다시 처리하려고 하면 예외 발생")
    void requestPayment_Fail_AlreadyProcessed(){
        // given
        PaymentRequest request = new PaymentRequest(1L, 1L, 1L, new BigDecimal("10000"));
        Payment payment = Payment.builder()
                .status(PaymentStatus.SUCCESS) // 이미 승인됨
                .build();

        given(paymentRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(payment));

        // when & then
        final RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.requestPayment(request)
        );
        assertThat(exception.getMessage()).contains("이미 처리 중이거나 완료된 주문");
    }
}
