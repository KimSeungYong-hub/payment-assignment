package com.practice.paymentassignment;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.domain.payment.PaymentRequest;
import com.practice.paymentassignment.domain.payment.service.PaymentService;
import com.practice.paymentassignment.domain.user.User;
import com.practice.paymentassignment.domain.user.UserService;
import com.practice.paymentassignment.domain.wallet.WalletService;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.global.exception.ErrorCode;
import com.practice.paymentassignment.global.exception.InsufficientBalanceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;


// 테스트 케이스가 부족합니다. 엣지 케이스와 실패 케이스를 포함한 충분한 케이스를 작성해주세요.
// 또한 테스트는 검증과 동시에 명세의 기능 또한 지닙니다. 
// 다른 팀원이 테스트 코드와 displayName을 읽고 해당 비즈니스 로직의 플로우를 파악할 수 있을 정도가 되어야합니다.
@ExtendWith(MockitoExtension.class)
class PaymentTransactionProcessorTest {

        @Mock
        private PaymentService paymentService;

        @Mock
        private UserService userService;

        @Mock
        private WalletService walletService;

        @InjectMocks
        private PaymentTransactionProcessor paymentTransactionProcessor;

        @Test
        @DisplayName("잔액 충분한 경우 결제 성공한다")
        void successPayment_WhenSufficientBalance() {
                // given
                PaymentDto.Approve.Request request = new PaymentDto.Approve.Request(1L, 1L, new BigDecimal("10000"));
                Long userId = 1L;

                Merchant merchant = Merchant.builder()
                                .merchantName("test_merchant")
                                .build();
                ReflectionTestUtils.setField(merchant, "id", 1L);

                PaymentRequest paymentRequest = PaymentRequest.builder()
                                .merchant(merchant)
                                .totalAmount(new BigDecimal("10000"))
                                .build();

                User user = User.builder()
                                .name("Tester")
                                .build();
                ReflectionTestUtils.setField(user, "id", 1L);

                given(paymentService.findPaymentRequestWithLock(1L)).willReturn(paymentRequest);
                given(userService.findUser(1L)).willReturn(user);

                // when
                paymentTransactionProcessor.successPayment(request, userId);

                // then
                verify(walletService).deduct(1L, new BigDecimal("10000"));
                verify(paymentService).recordSuccess(paymentRequest, user, new BigDecimal("10000"));
        }

        @Test
        @DisplayName("결제 시도 시 잔액 부족한 경우 InsufficientBalanceException 예외 발생")
        void successPayment_WhenInsufficientBalance_ThrowsException() {
                // given
                PaymentDto.Approve.Request request = new PaymentDto.Approve.Request(1L, 1L, new BigDecimal("10000"));
                Long userId = 1L;

                Merchant merchant = Merchant.builder()
                                .merchantName("test_merchant")
                                .build();
                ReflectionTestUtils.setField(merchant, "id", 1L);

                PaymentRequest paymentRequest = PaymentRequest.builder()
                                .merchant(merchant)
                                .totalAmount(new BigDecimal("10000"))
                                .build();

                User user = User.builder()
                                .name("Tester")
                                .build();
                ReflectionTestUtils.setField(user, "id", 1L);

                given(paymentService.findPaymentRequestWithLock(1L)).willReturn(paymentRequest);
                given(userService.findUser(1L)).willReturn(user);
                willThrow(new InsufficientBalanceException("잔액이 부족합니다."))
                                .given(walletService).deduct(1L, new BigDecimal("10000"));

                // when & then
                org.junit.jupiter.api.Assertions.assertThrows(
                                InsufficientBalanceException.class,
                                () -> paymentTransactionProcessor.successPayment(request, userId));
        }

        @Test
        @DisplayName("실패 기록 저장")
        void failPayment_SavesFailureRecord() {
                // given
                PaymentDto.Approve.Request request = new PaymentDto.Approve.Request(1L, 1L, new BigDecimal("10000"));
                Long userId = 1L;
                ErrorCode errorCode = ErrorCode.INSUFFICIENT_BALANCE;
                String message = "잔액이 부족합니다.";

                Merchant merchant = Merchant.builder()
                                .merchantName("test_merchant")
                                .build();
                ReflectionTestUtils.setField(merchant, "id", 1L);

                PaymentRequest paymentRequest = PaymentRequest.builder()
                                .merchant(merchant)
                                .totalAmount(new BigDecimal("10000"))
                                .build();

                User user = User.builder()
                                .name("Tester")
                                .build();
                ReflectionTestUtils.setField(user, "id", 1L);

                given(paymentService.findPaymentRequestWithLock(1L)).willReturn(paymentRequest);
                given(userService.findUser(1L)).willReturn(user);

                // when
                paymentTransactionProcessor.failPayment(request, userId, errorCode, message);

                // then
                verify(paymentService).recordFailure(
                                paymentRequest,
                                user,
                                new BigDecimal("10000"),
                                errorCode,
                                message);
        }

        @Test
        @DisplayName("결제 요청 저장")
        void savePaymentRequest_SavesPaymentRequest() {
                // given
                Merchant merchant = Merchant.builder()
                                .merchantName("test_merchant")
                                .build();
                String idempotencyKey = "test-key-123";

                PaymentDto.Prepare.Response expectedResponse = new PaymentDto.Prepare.Response(
                                "test_merchant",
                                new BigDecimal("10000"),
                                1L);

                given(paymentService.savePaymentRequest(merchant, idempotencyKey))
                                .willReturn(expectedResponse);

                // when
                PaymentDto.Prepare.Response response = paymentTransactionProcessor.savePaymentRequest(merchant,
                                idempotencyKey);

                // then
                verify(paymentService).savePaymentRequest(merchant, idempotencyKey);
        }
}
