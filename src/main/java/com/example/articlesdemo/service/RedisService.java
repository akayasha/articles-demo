package com.example.articlesdemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisService(StringRedisTemplate redisTemplate,
                        ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
            logger.debug("Redis SET operation successful. Key: {}, TTL: {} seconds", key, ttl.getSeconds());
        } catch (Exception e) {
            logger.error("Redis SET operation failed. Key: {}, TTL: {}", key, ttl.getSeconds(), e);
            throw new RuntimeException("Failed to set value in Redis", e);
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                logger.debug("Redis GET operation: Key not found - {}", key);
                return null;
            }
            T result = objectMapper.readValue(json, clazz);
            logger.debug("Redis GET operation successful. Key: {}, Type: {}", key, clazz.getSimpleName());
            return result;
        } catch (Exception e) {
            logger.warn("Redis GET operation failed. Key: {}, Type: {}", key, clazz.getSimpleName(), e);
            return null;
        }
    }

    public void delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(result)) {
                logger.debug("Redis DELETE operation successful. Key: {}", key);
            } else {
                logger.debug("Redis DELETE operation: Key not found or already deleted - {}", key);
            }
        } catch (Exception e) {
            logger.error("Redis DELETE operation failed. Key: {}", key, e);
        }
    }
}