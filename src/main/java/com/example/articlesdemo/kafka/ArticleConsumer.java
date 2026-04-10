package com.example.articlesdemo.kafka;

import com.example.articlesdemo.dto.request.ArticleRequest;
import com.example.articlesdemo.entity.Article;
import com.example.articlesdemo.repository.ArticleRepository;
import com.example.articlesdemo.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ArticleConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ArticleConsumer.class);

    private final ArticleRepository repository;
    private final RedisService redisService;

    public ArticleConsumer(ArticleRepository repository,
                           RedisService redisService) {
        this.repository = repository;
        this.redisService = redisService;
    }

    @KafkaListener(topics = "article-topic", groupId = "group-1")
    public void consume(ArticleRequest request) {
        logger.info("Consuming article message from Kafka. Title: {}", request.getTitle());

        try {
            // Check if article already exists
            if (repository.existsByTitle(request.getTitle())) {
                logger.warn("Duplicate article title detected and rejected: {}", request.getTitle());
                return;
            }

            Article article = Article.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .build();

            repository.save(article);
            logger.info("Article saved successfully. Title: {}, Description: {}",
                    request.getTitle(), request.getDescription());

            // Invalidate cache if exists
            redisService.delete("article:" + request.getTitle());
            logger.debug("Cache invalidated for article: {}", request.getTitle());

        } catch (DataIntegrityViolationException e) {
            logger.warn("DataIntegrityViolationException - Duplicate article title detected: {}",
                    request.getTitle(), e);
            // Log the constraint violation but don't re-throw to avoid consumer group issues
        } catch (Exception e) {
            logger.error("Unexpected error while consuming article message. Title: {}",
                    request.getTitle(), e);
            // Re-throw to trigger Kafka retry mechanism
            throw e;
        }
    }
}