package com.practice.paymentassignment.filter;

import com.practice.paymentassignment.dto.IdempotencyRedisResponse;
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

@Component
@RequiredArgsConstructor
// @WebFilter(urlPatterns = "/sft/approve")
public class IdempotencyFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Duration TTL = Duration.ofHours(24);

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String key = req.getHeader("Idempotency-Key");
        if (isWriteMethod(req) && key != null) {
            String redisKey = req.getMethod() + ":" + req.getRequestURI() + ":" + key;

            Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROGRESSING", TTL);

            if (Boolean.FALSE.equals(isFirstRequest)) {
                Object saved = redisTemplate.opsForValue().get(redisKey);

                if ("PROGRESSING".equals(saved)) {
                    res.setStatus(HttpServletResponse.SC_CONFLICT);
                    res.setContentType("application/json");
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write("{\"error\": \"현재 동일한 요청이 처리 중입니다.\"}");
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
                IdempotencyRedisResponse value = new IdempotencyRedisResponse(resp.getStatus(),
                        resp.getContentAsByteArray());
                redisTemplate.opsForValue().set(redisKey, value, TTL);
                resp.copyBodyToResponse();
            }
        } else {
            chain.doFilter(req, res);
        }
    }

    private boolean isWriteMethod(HttpServletRequest req) {
        return "POST".equalsIgnoreCase(req.getMethod());
    }
}
