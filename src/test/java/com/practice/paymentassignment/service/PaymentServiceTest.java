package com.practice.paymentassignment.service;

import com.practice.paymentassignment.dto.PaymentRequest;
import com.practice.paymentassignment.entity.*;
import com.practice.paymentassignment.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    private PaymentRequestRepository paymentRequestRepository;

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
        PaymentRequest request = new PaymentRequest( 1L, 1L, new BigDecimal("10000"));
        User user = new User(1L, "Tester");
        Long userId = user.getId();
        Wallet wallet = Wallet.builder()
                .id(1L)
                .user(user)
                .balance(new BigDecimal("15000"))
                .build();
        Merchant merchant = Merchant.builder()
                .merchantName("test_merchant")
                .build();
        ReflectionTestUtils.setField(merchant, "id", 1L);

        PaymentRequestEntity paymentRequest = PaymentRequestEntity.builder()
                .merchant(merchant)
                .totalAmount(new BigDecimal("10000"))
                .build();

        given(paymentRequestRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(paymentRequest));
        given(walletRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(wallet));
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        // when
        paymentService.requestPayment(request, userId);

        // then
        assertThat(wallet.getBalance()).isEqualByComparingTo("5000");
        assertThat(paymentRequest.getStatus()).isEqualTo(PaymentRequestStatus.SUCCESS);
    }

    @Test
    @DisplayName("결제 요청자와 주문자가 다를 경우 예외 발생")
    void requestPayment_Fail_InvalidUser(){
        // given
        PaymentRequest request = new PaymentRequest(999L, 1L, new BigDecimal("10000")); // user 999
        User correctUser = new User(1L, "Tester");
        Long userId = correctUser.getId();

        Wallet correctWallet = Wallet.builder().id(1L).user(correctUser).balance(new BigDecimal("10000")).build();

        Merchant merchant = Merchant.builder()
                .merchantName("test_merchant")
                .build();
        ReflectionTestUtils.setField(merchant, "id", 1L);

        PaymentRequestEntity paymentRequest = PaymentRequestEntity.builder()
                .merchant(merchant)

                .build();

        given(paymentRequestRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(paymentRequest));
        given(userRepository.findById(anyLong())).willReturn(Optional.of(correctUser));

        // when & then
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.requestPayment(request, userId)
        );
        assertThat(exception.getMessage()).contains("결제 요청자와 주문자가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("결제 요청 금액이 원장 금액과 다를 경우 (위변조 시도) 예외 발생")
    void requestPayment_Fail_InvalidAmount(){
        // given
        PaymentRequest request = new PaymentRequest(1L, 1L, new BigDecimal("1")); // 1원으로 조작
        User user = new User(1L, "Tester");
        Long userId = user.getId();
//        Wallet wallet = Wallet.builder().id(1L).user(user).balance(new BigDecimal("10000")).build();

        Merchant merchant = Merchant.builder()
                .merchantName("test_merchant")
                .build();
        ReflectionTestUtils.setField(merchant, "id", 1L);

        PaymentRequestEntity paymentRequest = PaymentRequestEntity.builder()
                .merchant(merchant)
                .orderId("test_order_Key")
                .totalAmount(new BigDecimal("10000")) // 진짜 원장 가격은 10000원
                .build();

        given(paymentRequestRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(paymentRequest));

        // when & then
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.requestPayment(request, userId)
        );
        assertThat(exception.getMessage()).contains("결제 요청 금액이 실제 주문 금액과 일치하지 않습니다");
    }

    @Test
    @DisplayName("유저 잔액이 부족할 경우 차감 시 예외 발생")
    void requestPayment_Fail_InsufficientBalance(){
        // given
        PaymentRequest request = new PaymentRequest(1L, 1L, new BigDecimal("10000"));
        User user = new User(1L, "Tester");
        Long userId = user.getId();

        Wallet wallet = Wallet.builder()
                .id(1L)
                .user(user)
                .balance(new BigDecimal("5000")) // 잔고 5000원 뿐
                .build();

        Merchant merchant = Merchant.builder()
                .merchantName("test_merchant")
                .build();
        ReflectionTestUtils.setField(merchant, "id", 1L);

        PaymentRequestEntity paymentRequestEntity = PaymentRequestEntity.builder()
                .merchant(merchant)
                .orderId("test_order_Key")
                .totalAmount(new BigDecimal("10000"))
                .build();

        Payment payment = Payment.createSuccess(paymentRequestEntity, user, new BigDecimal("10000"));

        given(paymentRequestRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(paymentRequestEntity));
        given(walletRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(wallet));

        // when & then
        final RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.requestPayment(request, userId)
        );
        assertThat(exception.getMessage()).contains("잔액이 부족");
    }

    @Test
    @DisplayName("이미 처리된 결제(SUCCESS)를 다시 처리하려고 하면 예외 발생")
    void requestPayment_Fail_AlreadyProcessed(){
        // given
        PaymentRequest request = new PaymentRequest(1L, 1L, new BigDecimal("10000"));
        User user = new User(1L, "Tester");
        Long userId = user.getId();

        Payment payment = Payment.builder()
                .status(PaymentStatus.SUCCESS) // 이미 승인됨
                .build();
        PaymentRequestEntity paymentRequest = PaymentRequestEntity.builder()
                .orderId("test_order_Key")
                .totalAmount(new BigDecimal("10000")) // 진짜 원장 가격은 10000원
                .build();
        paymentRequest.markAsDone();



        given(paymentRequestRepository.findByIdWithPessimisticLock(anyLong())).willReturn(Optional.of(paymentRequest));

        // when & then
        final RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentService.requestPayment(request, userId)
        );
        assertThat(exception.getMessage()).contains("이미 처리 중이거나 완료된 주문");
    }
}
