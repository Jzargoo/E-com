package com.jzargo.media.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("kafka")
public class KafkaConfig {

    private final KafkaPropertyStorage kafkaPropertyStorage;

    public KafkaConfig(KafkaPropertyStorage storage) {
        this.kafkaPropertyStorage = storage;
    }

    @Bean
    public NewTopic topicFileTransfer() {
        KafkaPropertyStorage.Topic fileTransferTopic = kafkaPropertyStorage.getFileTransferTopic();
        return TopicBuilder
                .name(
                        fileTransferTopic.getName()
                )
                .partitions(
                        fileTransferTopic.getOptions().getPartitions()
                )
                .replicas(
                        fileTransferTopic.getOptions().getReplication()
                )
                .config(
                        TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
                        fileTransferTopic.getOptions().getMinInSyncReplicas().toString()
                ).build();
    }

    @Bean
    public NewTopic topicSyncFiles() {
        KafkaPropertyStorage.Topic fileSyncTopic= kafkaPropertyStorage.getFileSyncTopic();
        return TopicBuilder
                .name(
                        fileSyncTopic.getName()
                )
                .partitions(
                        fileSyncTopic.getOptions().getPartitions()
                )
                .replicas(
                        fileSyncTopic.getOptions().getReplication()
                )
                .config(
                        TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
                        fileSyncTopic.getOptions().getMinInSyncReplicas().toString()
                ).build();
    }

}
