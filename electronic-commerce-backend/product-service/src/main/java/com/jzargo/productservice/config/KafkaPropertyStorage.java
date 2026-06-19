package com.jzargo.productservice.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@RefreshScope
@Validated
@ConfigurationProperties(prefix = "kafka")
@Data
public class KafkaPropertyStorage {
    private Topics topics;
    private String groupId;

    @Data
    public static class Topics{

        private TopicSettings productEventsTopic; // UPDATE, STATUS CHANGE
        private TopicSettings productCreateSaga;

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
