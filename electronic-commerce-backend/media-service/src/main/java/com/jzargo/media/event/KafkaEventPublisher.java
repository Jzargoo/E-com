package com.jzargo.media.event;

import com.jzargo.media.config.KafkaPropertyStorage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBooleanProperty("kafka.enabled")
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaPropertyStorage kafkaPropertyStorage;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, KafkaPropertyStorage kafkaPropertyStorage) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaPropertyStorage = kafkaPropertyStorage;
    }

    @Override
    public void publishFileRequestedEvent(FileRequestEvent event) {
        log.debug("publishFileRequestedEvent {}", event);

        String name = kafkaPropertyStorage.getFileTransferTopic().getName();
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(
                name,
                event.getFileURL(),
                event
        );

        producerRecord.headers().add(
                KafkaHeaders.RECEIVED_KEY, event.getFileURL().getBytes()
        );

        kafkaTemplate.send(producerRecord);

        log.info("Sent file with uri {} requested event to kafka topic: `{}", event.getFileURL(), name);
    }

    @Override
    public void publishFileCreatedSyncEvent(FileCreatedSyncEvent event) {
        log.debug("publishFileCreatedSyncEvent {}", event);

        String name = kafkaPropertyStorage.getFileSyncTopic().getName();

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(
                name,
                event.getFileUri(),
                event
        );

        producerRecord.headers().add(
                KafkaHeaders.RECEIVED_KEY, event.getFileUri().getBytes()
        );

        kafkaTemplate.send(producerRecord);

        log.info("Sent sync event to kafka topic with uri {} by {}", event.getFileUri(), event.getStorageType());
    }
}
