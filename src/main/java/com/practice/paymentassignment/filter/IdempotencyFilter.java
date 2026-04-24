package com.practice.paymentassignment.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.paymentassignment.dto.IdempotencyRedisResponse;
import com.practice.paymentassignment.exception.ErrorCode;
import com.practice.paymentassignment.exception.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

//@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Duration TTL = Duration.ofHours(24);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals("/sft/prepare") && !path.equals("/sft/approve");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String key = req.getHeader("Idempotency-Key");
        if(!isWriteMethod(req)){//|| key=null
            chain.doFilter(req, res);
            return;
        }

            String redisKey = req.getMethod() + ":" + req.getRequestURI() + ":" + key;
            // 저장 성공 시 (기존에 키가 없었음): 1 , 저장 실패 시 (기존에 키가 있었음): 0
            Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROGRESSING", TTL);

            if (Boolean.FALSE.equals(isFirstRequest)) {
                Object saved = redisTemplate.opsForValue().get(redisKey);

                if ("PROGRESSING".equals(saved)) {
                    res.setStatus(HttpServletResponse.SC_CONFLICT);
                    res.setContentType("application/json");
                    res.setCharacterEncoding("UTF-8");
                    ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.DATA_INTEGRITY_VIOLATION,
                            "이미 처리 중이거나 완료된 주문입니다.");
                    String jsonResponse = objectMapper.writeValueAsString(errorResponse);
                    res.getWriter().write(jsonResponse);
                    return;
                } else if (saved instanceof IdempotencyRedisResponse) {
                    IdempotencyRedisResponse cachedResponse = (IdempotencyRedisResponse) saved;
                    res.setStatus(cachedResponse.getStatus());
                    res.setContentType("application/json");
                    res.setCharacterEncoding("UTF-8");
                    res.getOutputStream().write(cachedResponse.getBody());
                    return;
                }
            }

            // 최초 처리
            ContentCachingResponseWrapper resp = new ContentCachingResponseWrapper(res);
            try {
                chain.doFilter(req, resp);
            } finally {
                int status = resp.getStatus();

                if (status >= 500) {
                    // 5xx (서버 에러, DB 다운 등) -> 락 삭제 (캐싱 X, 재시도 허용)
                    redisTemplate.delete(redisKey);
                } else {
                    // 200 OR 4xx (잔액 부족 등 클라이언트 에러) -> 캐싱 (O)
                    IdempotencyRedisResponse value = new IdempotencyRedisResponse(status, resp.getContentAsByteArray());
                    redisTemplate.opsForValue().set(redisKey, value, TTL);
                }

            }
    }

    private boolean isWriteMethod(HttpServletRequest req) {
        return "POST".equalsIgnoreCase(req.getMethod());
    }
}
