package com.jzargo.productservice.config;

import com.jzargo.core.command.createProductSaga.*;
import com.jzargo.productservice.entity.SagaStep;
import com.jzargo.productservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Configuration
@RefreshScope
@EnableKafka
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
