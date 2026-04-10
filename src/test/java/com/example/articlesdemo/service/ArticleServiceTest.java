package com.example.articlesdemo.service;

import com.example.articlesdemo.entity.Article;
import com.example.articlesdemo.repository.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository repository;
    @Mock
    private RedisService redisService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;

    @InjectMocks
    private ArticleService service;

    @Test
    void returnsFromCacheWhenPresent() {
        Article cached = Article.builder().id(1L).title("t").description("d").build();
        given(redisService.get("article:t", Article.class)).willReturn(cached);

        Article result = service.getByTitle("t");

        assertThat(result).isEqualTo(cached);
        verify(repository, never()).findByTitleIgnoreCase(anyString());
    }

    @Test
    void hitsDatabaseAndCachesOnMiss() throws InterruptedException {
        Article dbArticle = Article.builder().id(2L).title("t2").description("d2").build();
        given(redisService.get("article:t2", Article.class)).willReturn(null);
        given(redissonClient.getLock("lock:t2")).willReturn(lock);
        given(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);
        given(repository.findByTitleIgnoreCase("t2")).willReturn(Optional.of(dbArticle));

        Article result = service.getByTitle("t2");

        assertThat(result).isEqualTo(dbArticle);
        verify(redisService).set("article:t2", dbArticle, Duration.ofMinutes(10));
    }

    @Test
    void throwsWhenNotFound() throws InterruptedException {
        given(redisService.get("article:missing", Article.class)).willReturn(null);
        given(redissonClient.getLock("lock:missing")).willReturn(lock);
        given(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);
        given(repository.findByTitleIgnoreCase("missing")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByTitle("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
