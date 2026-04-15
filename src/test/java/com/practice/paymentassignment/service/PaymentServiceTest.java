package com.practice.paymentassignment.service;

import com.practice.paymentassignment.dto.PaymentRequest;
import com.practice.paymentassignment.entity.Merchant;
import com.practice.paymentassignment.entity.Payment;
import com.practice.paymentassignment.entity.PaymentStatus;
import com.practice.paymentassignment.entity.User;
import com.practice.paymentassignment.exception.MerchantNotFoundException;
import com.practice.paymentassignment.repository.MerchantRepository;
import com.practice.paymentassignment.repository.PaymentRepository;
import com.practice.paymentassignment.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("결제가 성공하면 유저 잔액이 깎이고 결제 내역이 저장되어야 한다.")
    void requestPayment_Success(){
        // given
        PaymentRequest request = new PaymentRequest(1L, 1L, 1L,"test_merchant", 10000, "test_orderId");
        User user = User.builder()
                .id(1L)
                .money(15000)
                .build();
        Merchant merchant = Merchant.builder()
                .id(1L)
                .merchantName("test_merchant")
                .build();
        Payment payment = Payment.builder()
                .user(user)
                .amount(request.getAmount())
                .status(PaymentStatus.READY)
                .merchant(merchant)
                .build();

        given(merchantRepository.existsByMerchantName(anyString())).willReturn(true); // 가맹점 통과
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(paymentRepository.existsByOrderId(anyString())).willReturn(true);
        given(paymentRepository.findByOrderId(anyString())).willReturn(payment);


        // when
        paymentService.requestPayment(request);

        // then
        assertThat(user.getMoney()).isEqualTo(5000);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

    }


    @Test
    @DisplayName("결제 요청 시 등록되지 않은 가맹점인 경우 예외 발생.")
    void requestPayment_Fail_InvalidMerchant(){
        // given
        PaymentRequest request = new PaymentRequest(1L, 1L, 1L, "not_exist_merchant", 10000, "test_orderId");
        given(merchantRepository.existsByMerchantName(anyString())).willReturn(false);
        given(paymentRepository.existsByOrderId(anyString())).willReturn(true);

        // when & then
        final MerchantNotFoundException exception = assertThrows(
                MerchantNotFoundException.class,
                () -> paymentService.requestPayment(request)
        );
        assertThat(exception.getMessage()).contains("유효하지 않은 가맹점입니다.");
        verify(paymentRepository, never()).save(any());
        verify(userRepository, never()).findById(anyLong());
    }



}
