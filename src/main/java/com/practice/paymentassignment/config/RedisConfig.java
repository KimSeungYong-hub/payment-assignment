package com.practice.paymentassignment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean("redisConfiguration")
    @ConditionalOnMissingBean(RedisConfiguration.class)
    public RedisStandaloneConfiguration redisStandaloneConfiguration(
            final RedisConnectionDetails connectionDetails
    ) {
        final RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        final RedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();

        config.setPassword(connectionDetails.getPassword());
        config.setUsername(connectionDetails.getUsername());
        config.setDatabase(standalone.getDatabase());
        config.setHostName(standalone.getHost());
        config.setPort(standalone.getPort());

        return config;
    }

    @Bean("redisConnectionFactory")
    public RedisConnectionFactory lettuceConnectionFactory(final RedisConfiguration redisConfiguration) {
        return new LettuceConnectionFactory(redisConfiguration);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            final RedisConnectionFactory redisConnectionFactory,
            final ObjectMapper objectMapper
    ) {
        final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }


}
