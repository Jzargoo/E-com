package com.jzargo.inventory.config.kafka;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "kafka")
@Component
@Validated
@ConditionalOnBooleanProperty("kafka.enabled")
@Data
public class KafkaPropertyStorage {

    @NotNull
    private Topics topics;

    @NotNull(message = "group id cannot be null")
    @NotEmpty(message =  "group id cannot be empty")
    private String groupId;

    @Data
    public static class Topics {
        @NotNull
        private TopicSettings productCreateSaga;

        @NotNull
        private String debeziumInventoryTopic;

        @Data
        public static class TopicSettings {
            @NotNull
            private String name;
            @NotNull
            private Integer numPartitions;
            @NotNull
            private Integer replicas;
            @NotNull
            private Integer inSyncReplicas;
        }

    }
}
