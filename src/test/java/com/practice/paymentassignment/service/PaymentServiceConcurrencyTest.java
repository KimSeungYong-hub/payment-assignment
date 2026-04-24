package com.practice.paymentassignment.service;

import com.practice.paymentassignment.AbstractIntegrationTest;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.entity.*;
import com.practice.paymentassignment.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentServiceConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    private User testUser;
    private Wallet testWallet;
    private Merchant testMerchant;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(new User(null, "Test User"));
        testWallet = walletRepository.save(Wallet.builder().user(testUser).balance(new BigDecimal("10000")).build());
        testMerchant = merchantRepository.save(Merchant.builder().merchantName("Test Merchant").build());
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        paymentRequestRepository.deleteAll();
        merchantRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("서로 다른 두 개의 결제를 거의 동시에 요청할 경우 비관적 락으로 인해 잔액이 방어되어야 한다.")
    void confirmPayment_Concurrent_DifferentOrders() throws InterruptedException {
        // given
        // 사용자 잔액은 10,000원인데 10,000원짜리 결제 2개를 동시에 생성
        PaymentRequestEntity payment1 = paymentRequestRepository.save(PaymentRequestEntity.builder()
                .merchant(testMerchant)
                .totalAmount(new BigDecimal("10000"))
                .orderId("order_1")
                .build());

        PaymentRequestEntity payment2 = paymentRequestRepository.save(PaymentRequestEntity.builder()
                .merchant(testMerchant)
                .totalAmount(new BigDecimal("10000"))
                .orderId("order_2")
                .build());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        executorService.submit(() -> {
            try {
                PaymentDto.Approve.Response response = paymentService.confirmPayment(
                        new PaymentDto.Approve.Request(payment1.getId(), testMerchant.getId(), new BigDecimal("10000")),
                        testUser.getId());
                if (response.isSuccess()) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                PaymentDto.Approve.Response response = paymentService.confirmPayment(
                        new PaymentDto.Approve.Request(payment2.getId(), testMerchant.getId(), new BigDecimal("10000")),
                        testUser.getId());
                if (response.isSuccess()) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);

        Wallet finalWallet = walletRepository.findById(testWallet.getId()).orElseThrow();
        assertThat(finalWallet.getBalance()).isEqualByComparingTo("0"); // 잔액이 마이너스가 아니어야 함
    }

    @Test
    @DisplayName("동일한 결제에 대해 다수의 요청이 동시에 들어와도 단 1번만 승인된다.")
    void confirmPayment_Concurrent_SameOrder() throws InterruptedException {
        // given
        PaymentRequestEntity payment = paymentRequestRepository.save(PaymentRequestEntity.builder()
                .merchant(testMerchant)
                .totalAmount(new BigDecimal("5000"))
                .orderId("order_sametest")
                .build());

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    PaymentDto.Approve.Response response = paymentService.confirmPayment(new PaymentDto.Approve.Request(
                            payment.getId(), testMerchant.getId(), new BigDecimal("5000")), testUser.getId());
                    if (response.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(9);

        Wallet finalWallet = walletRepository.findById(testWallet.getId()).orElseThrow();
        assertThat(finalWallet.getBalance()).isEqualByComparingTo("5000"); // 10000원에서 5000원 1번만 깎힘

        PaymentRequestEntity finalPaymentRequest = paymentRequestRepository.findById(payment.getId()).orElseThrow();
        assertThat(finalPaymentRequest.getStatus()).isEqualTo(PaymentRequestStatus.SUCCESS);
    }
}
