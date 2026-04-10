package com.example.articlesdemo.controller;

import com.example.articlesdemo.dto.request.ArticleRequest;
import com.example.articlesdemo.dto.response.ApiResponse;
import com.example.articlesdemo.entity.Article;
import com.example.articlesdemo.exception.DuplicateArticleException;
import com.example.articlesdemo.kafka.ArticleProducer;
import com.example.articlesdemo.service.ArticleService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private static final Logger logger = LoggerFactory.getLogger(ArticleController.class);

    private final ArticleService articleService;
    private final ArticleProducer articleProducer;

    public ArticleController(ArticleService articleService,
                             ArticleProducer articleProducer) {
        this.articleService = articleService;
        this.articleProducer = articleProducer;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Article>> getByTitle(
            @RequestParam String title) {
        logger.info("Fetching article with title: {}", title);
        try {
            Article article = articleService.getByTitle(title);
            logger.info("Successfully retrieved article: {}", title);
            return ResponseEntity.ok(ApiResponse.success(article));
        } catch (Exception e) {
            logger.error("Error fetching article with title: {}", title, e);
            throw e;
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> create(
            @Valid @RequestBody ArticleRequest request) {
        logger.info("Creating new article with title: {}", request.getTitle());
        try {
            // Check if article already exists BEFORE queuing
            if (articleProducer.isDuplicateTitle(request.getTitle())) {
                logger.warn("Duplicate article title detected in CREATE endpoint: {}", request.getTitle());
                return ResponseEntity.status(409)
                        .body(ApiResponse.error("Article with title '" + request.getTitle() + "' already exists"));
            }

            articleProducer.publish(request);
            logger.info("Article '{}' queued for processing", request.getTitle());
            return ResponseEntity.accepted()
                    .body(ApiResponse.success("Article queued for processing"));
        } catch (DuplicateArticleException e) {
            logger.warn("DuplicateArticleException caught: {}", request.getTitle());
            return ResponseEntity.status(409)
                    .body(ApiResponse.error("Article with title '" + request.getTitle() + "' already exists"));
        } catch (Exception e) {
            logger.error("Error queuing article: {}", request.getTitle(), e);
            throw e;
        }
    }
}