package com.practice.paymentassignment;

import com.practice.paymentassignment.dto.IdempotencyRedisResponse;
import com.practice.paymentassignment.exception.AlreadyProcessedException;
import com.practice.paymentassignment.exception.BusinessException;
import com.practice.paymentassignment.exception.ErrorCode;
import com.practice.paymentassignment.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.TimeToLive;
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

    @Around("@annotation(com.practice.paymentassignment.Idempotent)")
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
        String redisKey = idempotent.prefix()+ idempotencyHeader;

        Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROGRESSING", idempotent.ttl(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(isFirstRequest)) {
            log.warn("중복 요청 감지 - 멱등성 키: {}", redisKey);
            throw new AlreadyProcessedException("이미 처리 중이거나 완료된 주문입니다.");
        }

        try {
            return joinPoint.proceed();
        }catch (Exception e){
            if (e instanceof BusinessException) {
                throw e;
            }
            // 5xx (서버 에러, DB 다운 등) -> 락 삭제 (캐싱 X, 재시도 허용)
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

}
