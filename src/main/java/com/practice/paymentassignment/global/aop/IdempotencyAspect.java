package com.practice.paymentassignment.global.aop;

import com.practice.paymentassignment.global.annotation.Idempotent;
import com.practice.paymentassignment.global.exception.AlreadyProcessedException;
import com.practice.paymentassignment.global.exception.InsufficientBalanceException;
import com.practice.paymentassignment.global.exception.PaymentExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(com.practice.paymentassignment.global.annotation.Idempotent)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes==null){
            throw new IllegalStateException("웹 요청 컨텍스트를 찾을 수 없습니다.");
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        HttpServletRequest request = attributes.getRequest();
        String idempotencyHeader = request.getHeader("Idempotency-Key");

        if (idempotencyHeader == null || idempotencyHeader.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key 헤더가 필요합니다.");
        }

        String redisKey = idempotent.prefix() + idempotencyHeader;

        Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROGRESSING", idempotent.ttl(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(isFirstRequest)) {
            log.warn("중복 요청 감지 - 멱등성 키: {}", redisKey);
            throw new AlreadyProcessedException("이미 처리 중이거나 완료된 주문입니다.");
        }

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            // 키를 유지해야 하는 예외들 (재시료 불가능한 경우)
            if (e instanceof AlreadyProcessedException ||
                e instanceof InsufficientBalanceException ||
                e instanceof PaymentExpiredException) {
                throw e;
            }

            // 나머지 예외들은 키 삭제 (재시도 가능)
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

}
