package com.jzargo.media.event;

import com.jzargo.media.config.KafkaPropertyStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@ConditionalOnBooleanProperty("kafka.enabled")
@Component
public class KafkaDlqPublisher implements DlqPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaPropertyStorage kafkaPropertyStorage;

    public KafkaDlqPublisher(KafkaTemplate<String, Object> kafkaTemplate, KafkaPropertyStorage kafkaPropertyStorage) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaPropertyStorage = kafkaPropertyStorage;
    }

    @Override
    public void reserveUnprocessedEventUnavailableService(FileCreatedSyncEvent event) {
        String topicName = kafkaPropertyStorage.getFileSyncTopic().getName();

        kafkaTemplate.send(topicName, event);
    }
}
