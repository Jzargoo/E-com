package com.jzargo.productservice.config;

import com.jzargo.core.command.createProductSaga.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Slf4j
@Configuration
@RefreshScope
@EnableKafka
@ConditionalOnBooleanProperty("kafka.enabled")
public class KafkaConfig {

    private final KafkaPropertyStorage kafkaPropertyStorage;

    public KafkaConfig(KafkaPropertyStorage kafkaPropertyStorage) {
        this.kafkaPropertyStorage = kafkaPropertyStorage;
    }

    @Bean
    public NewTopic productEventsTopic() {

        var productEventsTopic = kafkaPropertyStorage // really long name
                .getTopics()
                .getProductEventsTopic();

        return TopicBuilder
                .name(
                        productEventsTopic.getName()
                )
                .replicas(
                        productEventsTopic.getReplicas()
                )
                .partitions(
                        productEventsTopic.getNumPartitions()
                )
                .config("min.insync.replicas",
                        productEventsTopic.getInSyncReplicas().toString()
                )
                .build();
    }

    @Bean
    public NewTopic productCreateSagaTopic() {
        var productCreateSagaTopic = kafkaPropertyStorage
                .getTopics()
                .getProductCreateSaga();

        return TopicBuilder
                .name(
                        productCreateSagaTopic.getName()
                )
                .replicas(
                        productCreateSagaTopic.getReplicas()
                )
                .partitions(
                        productCreateSagaTopic.getNumPartitions()
                )
                .config("min.insync.replicas",
                        productCreateSagaTopic.getInSyncReplicas().toString()
                )
                .build();
    }
}
