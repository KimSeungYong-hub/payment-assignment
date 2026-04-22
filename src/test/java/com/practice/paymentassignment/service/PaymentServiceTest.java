package com.practice.paymentassignment.service;

import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.entity.*;
import com.practice.paymentassignment.exception.PaymentForgeryException;
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
import static org.mockito.Mockito.when;

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
        @DisplayName("결제 준비 성공")
        public void preparePayment_Success() {
                // given
                PaymentDto.Prepare.Request request = new PaymentDto.Prepare.Request(1L, 1L);
                String idempotencyKey = "test-key";

                Merchant merchant = Merchant.builder()
                                .merchantName("Test Merchant")
                                .build();
                ReflectionTestUtils.setField(merchant, "id", 1L);

                when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));

                // when
                PaymentDto.Prepare.Response response = paymentService.preparePayment(request, idempotencyKey);

                // then
                assertThat(response.getMerchantName()).isEqualTo("Test Merchant");
                assertThat(response.getAmount()).isEqualByComparingTo("10000");
        }

        @Test
        @DisplayName("결제가 성공하면 유저 잔액이 깎이고 결제 내역이 저장되어야 한다.")
        public void requestPayment_Success() {
                // given
                PaymentDto.Approve.Request request = new PaymentDto.Approve.Request(1L, 1L, new BigDecimal("10000"));
                User user = new User(1L, "Tester");
                Wallet wallet = Wallet.builder()
                                .user(user)
                                .balance(new BigDecimal("15000"))
                                .build();
                ReflectionTestUtils.setField(wallet, "id", 1L);

                Merchant merchant = Merchant.builder()
                                .merchantName("test_merchant")
                                .build();
                ReflectionTestUtils.setField(merchant, "id", 1L);

                PaymentRequestEntity paymentRequest = PaymentRequestEntity.builder()
                                .merchant(merchant)
                                .totalAmount(new BigDecimal("10000"))
                                .build();

                given(paymentRequestRepository.findByIdWithPessimisticLock(anyLong()))
                                .willReturn(Optional.of(paymentRequest));
                given(walletRepository.findByUserIdWithPessimisticLock(anyLong())).willReturn(Optional.of(wallet));
                given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
                // when
                PaymentDto.Approve.Response response = paymentService.requestPayment(request, 1L);

                // then
                assertThat(response.isSuccess()).isTrue();
                assertThat(wallet.getBalance()).isEqualByComparingTo("5000");
                assertThat(paymentRequest.getStatus()).isEqualTo(PaymentRequestStatus.SUCCESS);
        }

        @Test
        @DisplayName("가맹점 정보가 일치하지 않는 경우 예외 발생")
        public void requestPayment_Merchant_Mismatch() {
                // given
                PaymentDto.Approve.Request request = new PaymentDto.Approve.Request(1L, 999L, new BigDecimal("10000")); // merchant
                                                                                                                        // 999
                User user = new User(1L, "Tester");
                Long userId = user.getId();

                Merchant merchant = Merchant.builder()
                                .merchantName("test_merchant")
                                .build();
                ReflectionTestUtils.setField(merchant, "id", 1L);

                PaymentRequestEntity paymentRequest = PaymentRequestEntity.builder()
                                .merchant(merchant)
                                .build();

                given(paymentRequestRepository.findByIdWithPessimisticLock(anyLong()))
                                .willReturn(Optional.of(paymentRequest));
                given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

                // when & then
                final PaymentForgeryException exception = assertThrows(
                                PaymentForgeryException.class,
                                () -> paymentService.requestPayment(request, userId));
                assertThat(exception.getMessage()).contains("가맹점 정보가 일치하지 않습니다");
        }

        @Test
        @DisplayName("결제 요청 금액이 원장 금액과 다를 경우 (위변조 시도) 예외 발생")
        public void requestPayment_Amount_Mismatch() {
                // given
                PaymentDto.Approve.Request request = new PaymentDto.Approve.Request(1L, 1L, new BigDecimal("1")); // 1원으로
                                                                                                                  // 조작
                User user = new User(1L, "Tester");
                Long userId = user.getId();

                Merchant merchant = Merchant.builder()
                                .merchantName("test_merchant")
                                .build();
                ReflectionTestUtils.setField(merchant, "id", 1L);

                PaymentRequestEntity paymentRequest = PaymentRequestEntity.builder()
                                .merchant(merchant)
                                .orderId("test_order_Key")
                                .totalAmount(new BigDecimal("10000")) // 진짜 원장 가격은 10000원
                                .build();

                given(paymentRequestRepository.findByIdWithPessimisticLock(anyLong()))
                                .willReturn(Optional.of(paymentRequest));
                given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

                // when & then
                final PaymentForgeryException exception = assertThrows(
                                PaymentForgeryException.class,
                                () -> paymentService.requestPayment(request, userId));
                assertThat(exception.getMessage()).contains("결제 요청 금액이 실제 주문 금액과 일치하지 않습니다");
        }

        @Test
        @DisplayName("유저 잔액이 부족할 경우 실패 응답을 반환하고 DB에 기록한다.")
        public void requestPayment_Insufficient_Balance() {
                // given
                PaymentDto.Approve.Request request = new PaymentDto.Approve.Request(1L, 1L, new BigDecimal("10000"));
                User user = new User(1L, "Tester");
                Long userId = user.getId();

                Wallet wallet = Wallet.builder()
                                .user(user)
                                .balance(new BigDecimal("5000")) // 잔고 5000원 뿐
                                .build();
                ReflectionTestUtils.setField(wallet, "id", 1L);

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

                given(paymentRequestRepository.findByIdWithPessimisticLock(anyLong()))
                                .willReturn(Optional.of(paymentRequestEntity));
                given(walletRepository.findByUserIdWithPessimisticLock(anyLong())).willReturn(Optional.of(wallet));
                given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

                // when
                PaymentDto.Approve.Response response = paymentService.requestPayment(request, userId);

                // then
                assertThat(response.isSuccess()).isFalse();
                assertThat(response.getMessage()).contains("잔액이 부족");
        }

        @Test
        @DisplayName("이미 처리된 결제(SUCCESS)를 다시 처리하려고 하면 예외 발생")
        void requestPayment_Fail_AlreadyProcessed() {
                // given
                PaymentDto.Approve.Request request = new PaymentDto.Approve.Request(1L, 1L, new BigDecimal("10000"));
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
                given(paymentRequestRepository.findByIdWithPessimisticLock(anyLong()))
                                .willReturn(Optional.of(paymentRequest));

                // when & then
                final RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> paymentService.requestPayment(request, userId));
                assertThat(exception.getMessage()).contains("이미 처리 중이거나 완료된 주문");
        }
}
