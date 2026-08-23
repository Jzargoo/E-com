package com.jzargo.productAssetsService.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@EnableKafka
@ConditionalOnBooleanProperty("kafka.enabled")
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic newTopic(KafkaPropertyStorage kafkaPropertyStorage) {

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
