package com.practice.paymentassignment.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.paymentassignment.AbstractIntegrationTest;
import com.practice.paymentassignment.dto.PaymentDto;
import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.domain.merchant.repository.MerchantRepository;
import com.practice.paymentassignment.domain.user.User;
import com.practice.paymentassignment.domain.user.repository.UserRepository;
import com.practice.paymentassignment.domain.wallet.Wallet;
import com.practice.paymentassignment.domain.wallet.repository.WalletRepository;
import com.practice.paymentassignment.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@SpringBootTest
@AutoConfigureMockMvc
public class IdempotentTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // static 변수로 실제 할당된 ID를 저장해둡니다.
    private static Long testUserId;
    private static Long testMerchantId;

    private User testUser;
    private Merchant testMerchant;

    @BeforeAll
    static void setupAll(@Autowired UserRepository userRepository,
                         @Autowired WalletRepository walletRepository,
                         @Autowired MerchantRepository merchantRepository) {

        User user = userRepository.save(new User(null, "Test User"));
        testUserId = user.getId();

        Merchant merchant = merchantRepository.save(Merchant.builder().merchantName("Test Merchant").build());
        testMerchantId = merchant.getId();

        walletRepository.save(Wallet.builder()
                .user(user)
                .balance(new BigDecimal("10000"))
                .build());
    }

    @BeforeEach
    void setUp() {
        User user = User.builder().name("Test User").build();
        setField(user, "id", testUserId);

        Merchant merchant = Merchant.builder().merchantName("Test Merchant").build();
        setField(merchant, "id", testMerchantId);

        walletRepository.findByUserId(testUserId)
                .ifPresent(wallet -> walletRepository.delete(wallet));

        walletRepository.save(Wallet.builder()
                .user(user)
                .balance(new BigDecimal("10000"))
                .build());

        testUser = user;
        testMerchant = merchant;
    }

    @AfterEach
    void tearDown() {
        // Redis만 비우기
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    @DisplayName("정상적인 첫 요청은 성공해야 한다.")
    void firstRequest_Success() throws Exception {
        // given
        String readyKey = "ready_key_first";
        PaymentDto.Prepare.Request readyRequest = new PaymentDto.Prepare.Request(testUser.getId(), testMerchant.getId());

        String readyResponse = mockMvc.perform(post("/sft/ready")
                        .header("Idempotency-Key", readyKey)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(readyRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long paymentId = objectMapper.readTree(readyResponse).get("paymentId").asLong();

        String confirmKey = "test_key_first";
        PaymentDto.Approve.Request confirmRequest = new PaymentDto.Approve.Request(paymentId, 1L,
                new BigDecimal("10000"));
        // when & then
        mockMvc.perform(post("/sft/confirm")
                .header("Idempotency-Key", confirmKey)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Idempotency-Key 헤더가 없는 경우 400 Bad Request를 반환한다.")
    void noIdempotencyKey_ThrowsException() throws Exception {
        // given
        PaymentDto.Approve.Request requestDto = new PaymentDto.Approve.Request(1L, testMerchant.getId(),
                new BigDecimal("10000"));

        // when & then
        mockMvc.perform(post("/sft/confirm")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Idempotency-Key 헤더가 빈 문자열인 경우 400 Bad Request를 반환한다.")
    void emptyIdempotencyKey_ThrowsException() throws Exception {
        // given
        PaymentDto.Approve.Request requestDto = new PaymentDto.Approve.Request(1L, testMerchant.getId(),
                new BigDecimal("10000"));

        // when & then
        mockMvc.perform(post("/sft/confirm")
                .header("Idempotency-Key", "")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("연속된 정상 요청 시 두 번째 요청은 409 Conflict를 반환해야 한다.")
    void consecutiveSecondRequest_ReturnsConflict() throws Exception {
        // given - 결제 요청 생성
        String readyKey = "ready_key_consecutive";
        PaymentDto.Prepare.Request readyRequest = new PaymentDto.Prepare.Request(testUser.getId(), testMerchant.getId());

        String readyResponse = mockMvc.perform(post("/sft/ready")
                        .header("Idempotency-Key", readyKey)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(readyRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long paymentId = objectMapper.readTree(readyResponse).get("paymentId").asLong();

        // when - 첫 번째 결제 승인 요청
        String confirmKey = "test_key_consecutive";
        PaymentDto.Approve.Request confirmRequest = new PaymentDto.Approve.Request(paymentId, 1L,
                new BigDecimal("10000"));

        mockMvc.perform(post("/sft/confirm")
                .header("Idempotency-Key", confirmKey)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());

        // then - 두 번째 요청 (Redis 키가 삭제되지 않았으므로 Conflict)
        mockMvc.perform(post("/sft/confirm")
                .header("Idempotency-Key", confirmKey)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DATA_INTEGRITY_VIOLATION.getCode()))
                .andExpect(jsonPath("$.message").value("이미 처리 중이거나 완료된 주문입니다."));
    }

    @Test
    @DisplayName("비즈니스 예외(404) 발생 시 Redis 키가 삭제되어야 한다.")
    void businessException_DeletesRedisKey() throws Exception {
        // given
        String idempotencyKey = "test_key_404";
        String redisKey = "idempotency:confirmPayment:" + idempotencyKey;
        PaymentDto.Approve.Request requestDto = new PaymentDto.Approve.Request(999L, 999L,
                new BigDecimal("10000"));

        // when - 404 발생
        mockMvc.perform(post("/sft/confirm")
                .header("Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());

        // then - Redis 키가 삭제되었는지 확인
        Boolean keyExists = redisTemplate.hasKey(redisKey);
        assertThat(keyExists).isFalse();
    }

    @Test
    @DisplayName("404 후 동일한 키로 재요청 시 다시 404가 반환되어야 한다 (키가 삭제되었으므로 재시도 가능).")
    void after404_RetryWithSameKey_Returns404Again() throws Exception {
        // given
        String idempotencyKey = "test_key_retry";
        PaymentDto.Approve.Request requestDto = new PaymentDto.Approve.Request(999L, 999L,
                new BigDecimal("10000"));

        // when - 첫 번째 요청 실패 (404)
        mockMvc.perform(post("/sft/confirm")
                .header("Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());

        // then - 동일한 키로 재요청 (키가 삭제되었으므로 다시 404)
        mockMvc.perform(post("/sft/confirm")
                .header("Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());
    }
}