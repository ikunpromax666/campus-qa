package com.campus.campusqaweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * ClassName: RedisConfig
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/24 10:35
 * Version:1.0
 */
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate (RedisConnectionFactory factory) {
        RedisTemplate <String, Object> template = new RedisTemplate <>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer ();
        template.setValueSerializer(json);
        template.setHashValueSerializer(json);
        template.afterPropertiesSet();
        return template;
    }
}
