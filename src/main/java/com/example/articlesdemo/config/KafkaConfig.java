package com.example.articlesdemo.config;

import com.example.articlesdemo.dto.request.ArticleRequest;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC_NAME = "article-topic";

    @Bean
    public ProducerFactory<String, ArticleRequest> producerFactory() {
        logger.info("Initializing Kafka ProducerFactory with bootstrap servers: {}", BOOTSTRAP_SERVERS);

        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Enable idempotence for exactly-once semantics
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-producer");

        logger.info("Producer configuration initialized successfully");
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, ArticleRequest> kafkaTemplate() {
        logger.info("Creating KafkaTemplate bean");
        KafkaTemplate<String, ArticleRequest> template =
                new KafkaTemplate<>(producerFactory());

        template.setTransactionIdPrefix("tx-producer");
        logger.info("KafkaTemplate created with transaction prefix: tx-producer");
        return template;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ArticleRequest> kafkaListenerContainerFactory() {
        logger.info("Initializing KafkaListenerContainerFactory");

        DefaultKafkaConsumerFactory<String, ArticleRequest> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerConfigs());

        ConcurrentKafkaListenerContainerFactory<String, ArticleRequest> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConcurrency(3);
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.setConsumerFactory(consumerFactory);

        logger.info("KafkaListenerContainerFactory configured with concurrency: 3");
        return factory;
    }

    private Map<String, Object> consumerConfigs() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "group-1");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ArticleRequest.class.getName());
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return config;
    }

    @Bean
    public NewTopic articleTopic() {
        logger.info("Creating Kafka topic: {}", TOPIC_NAME);
        return TopicBuilder.name(TOPIC_NAME)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "86400000") // 24 hours
                .build();
    }
}