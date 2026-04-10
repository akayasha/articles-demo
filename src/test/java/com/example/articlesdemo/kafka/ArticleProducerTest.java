package com.example.articlesdemo.kafka;

import com.example.articlesdemo.dto.request.ArticleRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleProducerTest {

    @Mock
    private KafkaTemplate<String, ArticleRequest> kafkaTemplate;

    @InjectMocks
    private ArticleProducer producer;

    @Test
    void publishesMessageWithTitleKey() {
        ArticleRequest request = ArticleRequest.builder()
                .title("hello")
                .description("world")
                .build();

        // Mock transaction execution
        when(kafkaTemplate.executeInTransaction(any()))
                .thenAnswer(invocation -> {
                    KafkaOperations.OperationsCallback<String, ArticleRequest, ?> callback =
                            invocation.getArgument(0);

                    return callback.doInOperations(kafkaTemplate);
                });

        // Mock Kafka send result properly
        CompletableFuture<SendResult<String, ArticleRequest>> future =
                mock(CompletableFuture.class);

        when(kafkaTemplate.send("article-topic", "hello", request))
                .thenReturn(future);

        // simulate async success (important because your code uses whenComplete)
        when(future.whenComplete(any()))
                .thenReturn(future);

        producer.publish(request);

        verify(kafkaTemplate).executeInTransaction(any());
        verify(kafkaTemplate).send("article-topic", "hello", request);
    }
}
