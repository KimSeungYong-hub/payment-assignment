package com.practice.paymentassignment.filter;

import com.practice.paymentassignment.dto.IdempotencyRedisResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class IdempotencyFilterTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.2")
            .withExposedPorts(6379);

    @Container
    static org.testcontainers.containers.MariaDBContainer<?> mariaDBContainer = new org.testcontainers.containers.MariaDBContainer<>(
            "mariadb:10.11")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.datasource.url", mariaDBContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDBContainer::getUsername);
        registry.add("spring.datasource.password", mariaDBContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    @DisplayName("멱등성 캐시가 존재할 때 기존 응답(PaymentApproveResponse)을 반환한다.")
    void filter_ReturnsCachedResponse() throws Exception {
        // given
        String idempotencyKey = "test_key_ok";
        String redisKey = "POST:/sft/approve:" + idempotencyKey;

        // 실제 컨트롤러가 반환하는 PaymentApproveResponse 형태로 캐시 세팅
        com.practice.paymentassignment.dto.PaymentApproveResponse mockResponse = com.practice.paymentassignment.dto.PaymentApproveResponse.of(true, "결제가 완료되었습니다.");
        IdempotencyRedisResponse cachedResponse = new IdempotencyRedisResponse(
                200, objectMapper.writeValueAsBytes(mockResponse)
        );
        redisTemplate.opsForValue().set(redisKey, cachedResponse);

        // 실제 PaymentRequest 객체 생성
        com.practice.paymentassignment.dto.PaymentRequest requestDto = 
                new com.practice.paymentassignment.dto.PaymentRequest(1L, 1L, new java.math.BigDecimal("10000.00"));

        // when & then
        mockMvc.perform(post("/sft/approve")
                .header("Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("결제가 완료되었습니다."));
    }

    @Test
    @DisplayName("동일한 요청이 PROGRESSING 상태일 경우 409 Conflict를 반환한다.")
    void filter_ReturnsConflictWhenProgressing() throws Exception {
        // given
        String idempotencyKey = "test_key_conflict";
        String redisKey = "POST:/sft/approve:" + idempotencyKey;

        // 다른 스레드에서 처리 중인 상태로 설정
        redisTemplate.opsForValue().set(redisKey, "PROGRESSING");

        // when & then
        mockMvc.perform(post("/sft/approve")
                .header("Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .content("{\"paymentId\": 1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("현재 동일한 요청이 처리 중입니다."));
    }
}
