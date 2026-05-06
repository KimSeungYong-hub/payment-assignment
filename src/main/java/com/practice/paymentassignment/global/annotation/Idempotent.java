package com.practice.paymentassignment.global.annotation;

import java.lang.annotation.*;

//@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    // Redis 키의 접두사 (업무 도메인마다 분리하기 위함)
    String prefix() default "idempotency:";

    // Redis 키의 TTL (초 단위, 기본 24시간)
    long ttl() default 86400;
}
