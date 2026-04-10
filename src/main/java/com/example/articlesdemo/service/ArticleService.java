package com.example.articlesdemo.service;

import com.example.articlesdemo.entity.Article;
import com.example.articlesdemo.repository.ArticleRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class ArticleService {

    private static final Logger logger = LoggerFactory.getLogger(ArticleService.class);

    private final ArticleRepository repository;
    private final RedisService redisService;
    private final RedissonClient redissonClient;

    public ArticleService(ArticleRepository repository,
                          RedisService redisService,
                          RedissonClient redissonClient) {
        this.repository = repository;
        this.redisService = redisService;
        this.redissonClient = redissonClient;
    }

    public Article getByTitle(String title) {
        logger.info("Attempting to fetch article by title: {}", title);

        String cacheKey = "article:" + title;

        // Try to get from cache first
        Article cached = redisService.get(cacheKey, Article.class);
        if (cached != null) {
            logger.info("Article found in cache: {}", title);
            return cached;
        }

        logger.debug("Article not in cache, acquiring lock for title: {}", title);
        RLock lock = redissonClient.getLock("lock:" + title);

        try {
            // Try to acquire lock with 2 second wait time and 5 second expiration
            if (lock.tryLock(2, 5, TimeUnit.SECONDS)) {
                logger.debug("Lock acquired successfully for title: {}", title);

                // Double-check cache after acquiring lock
                Article cachedAfterLock = redisService.get(cacheKey, Article.class);
                if (cachedAfterLock != null) {
                    logger.info("Article found in cache after lock acquisition: {}", title);
                    return cachedAfterLock;
                }

                // Fetch from database
                Article article = repository.findByTitleIgnoreCase(title)
                        .orElseThrow(() -> {
                            logger.warn("Article not found in database: {}", title);
                            return new ResourceNotFoundException("Article not found with title: " + title);
                        });

                logger.info("Article loaded from database: {}", title);

                // Cache the result
                redisService.set(cacheKey, article, Duration.ofMinutes(10));
                logger.debug("Article cached with 10 minute TTL: {}", title);

                return article;

            } else {
                logger.warn("Failed to acquire lock within timeout for title: {}", title);
                // Fallback: fetch directly from database without lock
                return repository.findByTitleIgnoreCase(title)
                        .orElseThrow(() -> {
                            logger.warn("Article not found in database (fallback): {}", title);
                            return new ResourceNotFoundException("Article not found with title: " + title);
                        });
            }

        } catch (InterruptedException e) {
            logger.error("Lock acquisition interrupted for title: {}", title, e);
            Thread.currentThread().interrupt();
            // Fallback to direct database fetch
            return repository.findByTitleIgnoreCase(title)
                    .orElseThrow(() -> new ResourceNotFoundException("Article not found with title: " + title));
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.debug("Lock released for title: {}", title);
            }
        }
    }
}