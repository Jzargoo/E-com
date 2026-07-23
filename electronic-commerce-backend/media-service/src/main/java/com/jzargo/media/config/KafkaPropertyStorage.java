package com.jzargo.media.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Data
@Component
@Profile("kafka")
@NoArgsConstructor
@AllArgsConstructor
@ConditionalOnBooleanProperty("kafka.enabled")
@ConfigurationProperties(prefix = "kafka")
public class KafkaPropertyStorage {
    // Topic that publish from primary storage into secondary(archives)
    private Topic fileTransferTopic;
    // Topic that synchronize files from all the secondary storages
    private Topic fileSyncTopic;
    // dlq topic to handle when a business logic is failed
    private Topic failedFileOperationTopic;
    // Topic that notify when a certain service can process requests
    private Topic storageRecoveryTopic;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Topic {
        private String name;
        private Options options;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Options {
            private Integer partitions;
            private Integer replication;
            private Integer minInSyncReplicas;
        }
    }
}
