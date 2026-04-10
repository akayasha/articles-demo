package com.example.articlesdemo.kafka;

import com.example.articlesdemo.dto.request.ArticleRequest;
import com.example.articlesdemo.entity.Article;
import com.example.articlesdemo.repository.ArticleRepository;
import com.example.articlesdemo.service.RedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleConsumerTest {

    @Mock
    private ArticleRepository repository;
    @Mock
    private RedisService redisService;

    @InjectMocks
    private ArticleConsumer consumer;

    @Test
    void savesToDatabaseAndEvictsCache() {
        ArticleRequest request = ArticleRequest.builder()
                .title("title")
                .description("desc")
                .build();

        consumer.consume(request);

        verify(repository).save(any(Article.class));
        verify(redisService).delete("article:title");
    }

    @Test
    void ignoresDuplicate() {
        ArticleRequest request = ArticleRequest.builder()
                .title("title")
                .description("desc")
                .build();

        doThrow(new DataIntegrityViolationException("dup")).when(repository).save(any(Article.class));

        consumer.consume(request);

        verify(redisService, never()).delete(anyString());
    }
}
