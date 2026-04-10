package com.example.articlesdemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RedisService redisService;

    @Test
    void setSerializesObject() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        redisService = new RedisService(redisTemplate, objectMapper);

        Dummy dummy = new Dummy("x");
        redisService.set("k", dummy, Duration.ofSeconds(5));

        String expectedJson = objectMapper.writeValueAsString(dummy);
        verify(valueOps).set(eq("k"), eq(expectedJson), eq(Duration.ofSeconds(5)));
    }

    @Test
    void getDeserializesObject() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        redisService = new RedisService(redisTemplate, objectMapper);
        Dummy dummy = new Dummy("y");
        when(valueOps.get("k")).thenReturn(objectMapper.writeValueAsString(dummy));

        Dummy result = redisService.get("k", Dummy.class);

        assertThat(result.value).isEqualTo("y");
    }

    private record Dummy(String value) {}
}
