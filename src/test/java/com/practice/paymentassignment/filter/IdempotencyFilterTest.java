package com.practice.paymentassignment.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.paymentassignment.AbstractIntegrationTest;
import com.practice.paymentassignment.dto.IdempotencyRedisResponse;
import com.practice.paymentassignment.dto.PaymentDto;

import com.practice.paymentassignment.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class IdempotencyFilterTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("멱등성 캐시가 존재할 때 기존 응답(PaymentApproveResponse)을 반환한다.")
    void filter_ReturnsCachedResponse() throws Exception {
        // given
        String idempotencyKey = "test_key_ok";
        String redisKey = "POST:/sft/approve:" + idempotencyKey;

        // 실제 컨트롤러가 반환하는 PaymentApproveResponse 형태로 캐시 세팅
        PaymentDto.Approve.Response mockResponse = PaymentDto.Approve.Response.of(true, "결제가 완료되었습니다.");
        IdempotencyRedisResponse cachedResponse = new IdempotencyRedisResponse(
                200, objectMapper.writeValueAsBytes(mockResponse));
        redisTemplate.opsForValue().set(redisKey, cachedResponse);

        // 실제 PaymentRequest 객체 생성
        PaymentDto.Approve.Request requestDto = new PaymentDto.Approve.Request(1L, 1L,
                new java.math.BigDecimal("10000"));

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

        PaymentDto.Approve.Request requestDto = new PaymentDto.Approve.Request(1L, 1L,
                new java.math.BigDecimal("10000"));
        // 다른 스레드에서 처리 중인 상태로 설정
        redisTemplate.opsForValue().set(redisKey, "PROGRESSING");

        // when & then
        mockMvc.perform(post("/sft/approve")
                .header("Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DATA_INTEGRITY_VIOLATION.getCode()))
                .andExpect(jsonPath("$.message").value("이미 처리 중이거나 완료된 주문입니다."));
    }
}
