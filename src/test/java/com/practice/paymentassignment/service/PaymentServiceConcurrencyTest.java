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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class PaymentServiceConcurrencyTest {

    @Container
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>("mariadb:10.11")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDBContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDBContainer::getUsername);
        registry.add("spring.datasource.password", mariaDBContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

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
        merchantRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("서로 다른 두 개의 결제를 거의 동시에 요청할 경우 비관적 락으로 인해 잔액이 방어되어야 한다.")
    void requestPayment_Concurrent_DifferentOrders() throws InterruptedException {
        // given
        // 사용자 잔액은 10,000원인데 10,000원짜리 결제 2개를 동시에 생성
        Payment payment1 = paymentRepository.save(Payment.builder()
                .wallet(testWallet)
                .merchant(testMerchant)
                .amount(new BigDecimal("10000"))
                .status(PaymentStatus.READY)
                .orderId("order_1")
                .build());

        Payment payment2 = paymentRepository.save(Payment.builder()
                .merchant(testMerchant)
                .amount(new BigDecimal("10000"))
                .status(PaymentStatus.READY)
                .orderId("order_2")
                .build());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        executorService.submit(() -> {
            try {
                paymentService.requestPayment(new PaymentRequest(testUser.getId(), payment1.getId(), testMerchant.getId(), new BigDecimal("10000")));
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                paymentService.requestPayment(new PaymentRequest(testUser.getId(), payment2.getId(), testMerchant.getId(), new BigDecimal("10000")));
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        // then
        // 하나의 결제만 성공하고, 나머지 하나는 잔액 부족으로 실패해야 한다.
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);

        Wallet finalWallet = walletRepository.findById(testWallet.getId()).orElseThrow();
        assertThat(finalWallet.getBalance()).isEqualByComparingTo("0"); // 잔액이 마이너스가 아니어야 함
    }

    @Test
    @DisplayName("동일한 결제에 대해 다수의 요청이 동시에 들어와도 단 1번만 승인된다.")
    void requestPayment_Concurrent_SameOrder() throws InterruptedException {
        // given
        Payment payment = paymentRepository.save(Payment.builder()
                .wallet(testWallet)
                .merchant(testMerchant)
                .amount(new BigDecimal("5000"))
                .status(PaymentStatus.READY)
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
                    paymentService.requestPayment(new PaymentRequest(testUser.getId(), payment.getId(), testMerchant.getId(), new BigDecimal("5000")));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("TEST FAIL REASON: " + e.getMessage());
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

        Payment finalPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
