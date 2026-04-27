package com.practice.paymentassignment.config;

import com.practice.paymentassignment.filter.IdempotencyFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@RequiredArgsConstructor
public class FilterConfig {
    private final RedisTemplate<String, Object> redisTemplate;

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> filterRegistrationBean(){
        FilterRegistrationBean<IdempotencyFilter> filterRegistrationBean = new FilterRegistrationBean<>(new IdempotencyFilter(redisTemplate));
        filterRegistrationBean.addUrlPatterns("/sft/ready", "/sft/confirm");
        System.out.println("filterRegistrationBean: test filter!!!!!!!!!!!!!!!!!!!!!");
        return filterRegistrationBean;
    }
}
