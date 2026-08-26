package com.jzargo.inventory.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
@EnableKafka
@ConditionalOnBooleanProperty("kafka.enabled")
public class KafkaConfig {

    @Bean
    public NewTopic sagaTopic(KafkaPropertyStorage kafkaPropertyStorage) {

        var productCreateSagaTopic = kafkaPropertyStorage.getTopics()
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
                .config(
                        TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
                        productCreateSagaTopic.getInSyncReplicas().toString()
                )
                .build();

    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> manualListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL);

        factory.setConsumerFactory(consumerFactory);


        return factory;
    }

}
