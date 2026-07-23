package com.jzargo.media.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableKafka
@ConditionalOnBooleanProperty("kafka.enabled")
public class KafkaConfig {

    private final KafkaPropertyStorage kafkaPropertyStorage;
    private final ApplicationPropertyStorage applicationPropertyStorage;

    public KafkaConfig(KafkaPropertyStorage storage, ApplicationPropertyStorage applicationPropertyStorage) {
        this.kafkaPropertyStorage = storage;
        this.applicationPropertyStorage = applicationPropertyStorage;
    }

    @Bean
    public NewTopic topicFileTransfer() {
        return createTopicByProperties(kafkaPropertyStorage.getFileTransferTopic(), "");
    }

    @Bean
    public NewTopic topicSyncFiles() {
        return createTopicByProperties(kafkaPropertyStorage.getFileSyncTopic(), "");
    }

    @Bean
    public NewTopic topicStoragesRecovery() {
        return createTopicByProperties(kafkaPropertyStorage.getStorageRecoveryTopic(), "");
    }

    @Bean
    public KafkaAdmin.NewTopics DLQsByStorage(){

        List<NewTopic> topics = new ArrayList<>();

        KafkaPropertyStorage.Topic failedFileOperationTopic =
                kafkaPropertyStorage.getFailedFileOperationTopic();

        applicationPropertyStorage.getStorages().forEach(
                storage -> topics.add(
                    createTopicByProperties(
                            failedFileOperationTopic,
                            "_" + storage.getStorageType().toString()
                    )
                )
        );

        return new KafkaAdmin.NewTopics(
                topics.toArray(NewTopic[]::new)
        );
    }

    private NewTopic createTopicByProperties(KafkaPropertyStorage.Topic topic, String namePostfix) {
        return TopicBuilder
                .name(
                        topic.getName() + namePostfix
                )
                .partitions(
                        topic.getOptions().getPartitions()
                )
                .replicas(
                        topic.getOptions().getReplication()
                )
                .config(
                        TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
                        topic.getOptions().getMinInSyncReplicas().toString()
                ).build();
    }

}
