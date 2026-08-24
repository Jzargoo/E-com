package com.jzargo.productAssetsService.config.kafka;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;


@ConfigurationProperties(prefix = "kafka")
@RefreshScope
@Component
@Data
@ConditionalOnBooleanProperty("kafka.enabled")
public class KafkaPropertyStorage {

    private Topics topics;
    private String groupId;

    @Data
    public static class Topics{

        @NotNull
        private TopicSettings productCreateSaga;
        @NotNull
        @NotEmpty(message = "debezium topic name cannot be empty or null")
        private String productAssetsDebeziumTopicName;

        @Data
        public static class TopicSettings{
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
