package com.practice.paymentassignment.filter;

import com.practice.paymentassignment.dto.IdempotencyRedisResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
//@WebFilter(urlPatterns = "/sft/approve")
public class IdempotencyFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Duration TTL = Duration.ofHours(24);

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String key = req.getHeader("Idempotency-Key");
        if (isWriteMethod(req) && key != null) {
//            var saved = store.find(key, req.getRequestURI());
            IdempotencyRedisResponse saved = (IdempotencyRedisResponse) redisTemplate.opsForValue().get(key);

            if (saved!=null) { // 재호출
                res.setStatus(saved.status());
                res.getOutputStream().write(saved.body());
                return; // 컨트롤러 미진입
            }
        }
        // 최초 처리
        ContentCachingResponseWrapper resp = new ContentCachingResponseWrapper(res);
        try { chain.doFilter(req, resp); }
        finally {
            if (isWriteMethod(req) && key != null) {
                IdempotencyRedisResponse value = new IdempotencyRedisResponse(resp.getStatus(), resp.getContentAsByteArray());
                redisTemplate.opsForValue().set(key, value, TTL);
            }
            resp.copyBodyToResponse();
        }
    }

    private boolean isWriteMethod(HttpServletRequest req) {
        return "POST".equalsIgnoreCase(req.getMethod());
    }
}
