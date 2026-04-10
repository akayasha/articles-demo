package com.example.articlesdemo.kafka;

import com.example.articlesdemo.dto.request.ArticleRequest;
import com.example.articlesdemo.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ArticleProducer {

    private static final Logger logger = LoggerFactory.getLogger(ArticleProducer.class);

    private final KafkaTemplate<String, ArticleRequest> kafkaTemplate;
    private final ArticleRepository articleRepository;

    public ArticleProducer(KafkaTemplate<String, ArticleRequest> kafkaTemplate,
                           ArticleRepository articleRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.articleRepository = articleRepository;
    }

    public boolean isDuplicateTitle(String title) {
        boolean exists = articleRepository.existsByTitle(title);
        logger.debug("Duplicate check for title '{}': {}", title, exists);
        return exists;
    }

    public void publish(ArticleRequest request) {
        logger.info("Publishing article to Kafka topic. Title: {}", request.getTitle());

        try {
            kafkaTemplate.executeInTransaction(kt -> {
                var sendResult = kt.send("article-topic", request.getTitle(), request);

                sendResult.whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Article published successfully. Title: {}, Partition: {}, Offset: {}",
                                request.getTitle(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        logger.error("Failed to publish article. Title: {}", request.getTitle(), ex);
                    }
                });

                return true;
            });
        } catch (Exception e) {
            logger.error("Error during Kafka transaction. Title: {}", request.getTitle(), e);
            throw e;
        }
    }
}