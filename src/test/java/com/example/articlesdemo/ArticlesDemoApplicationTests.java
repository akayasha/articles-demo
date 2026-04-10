package com.example.articlesdemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import com.example.articlesdemo.repository.ArticleRepository;
import com.example.articlesdemo.dto.request.ArticleRequest;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;

@SpringBootTest
@ActiveProfiles("test")
@ImportAutoConfiguration(exclude = {RedissonAutoConfiguration.class, RedissonAutoConfigurationV2.class})
class ArticlesDemoApplicationTests {

    @MockBean
    RedissonClient redissonClient;
    @MockBean
    StringRedisTemplate stringRedisTemplate;
    @MockBean
    KafkaTemplate<String, ArticleRequest> kafkaTemplate;
    @MockBean
    ArticleRepository articleRepository;

    @Test
    void contextLoads() {
    }
}
