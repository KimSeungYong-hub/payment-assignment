package com.practice.paymentassignment.global.aop;

import com.practice.paymentassignment.global.annotation.IdempotentRequest;
import com.practice.paymentassignment.global.exception.AlreadyProcessedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
@Slf4j
@Aspect
@Component
@Order(1)//db 연결 전에 먼저 실행 위해
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(com.practice.paymentassignment.global.annotation.IdempotentRequest)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes==null){
            throw new IllegalStateException("웹 요청 컨텍스트를 찾을 수 없습니다.");
        }

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        IdempotentRequest idempotentRequest = AnnotationUtils.findAnnotation(method, IdempotentRequest.class);

        HttpServletRequest request = attributes.getRequest();
        String idempotencyHeader = request.getHeader("Idempotency-Key");

        if (idempotencyHeader == null || idempotencyHeader.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key 헤더가 필요합니다.");
        }

        String redisKey = idempotentRequest.prefix() + idempotencyHeader;

        Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(redisKey, "LOCKED", idempotentRequest.ttl(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(isFirstRequest)) {
            log.warn("중복 요청 감지 - 멱등성 키: {}", redisKey);
            throw new AlreadyProcessedException("이미 처리 중이거나 완료된 주문입니다.");
        }

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

}
