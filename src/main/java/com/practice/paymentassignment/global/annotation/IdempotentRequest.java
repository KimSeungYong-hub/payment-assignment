package com.practice.paymentassignment.global.annotation;

import java.lang.annotation.*;

// Idempotent는 멱등성 이라는 뜻입니다. 
// 멱등성은 성질을 뜻하는 것이기에, 주석(annotastion)의 명으로는 어울리지 않을 것 같습니다.
//@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentRequest {
    // Redis 키의 접두사 (업무 도메인마다 분리하기 위함)
    String prefix() default "idempotency:";

    // Redis 키의 TTL (초 단위, 기본 24시간)
    long ttl() default 86400;
}
