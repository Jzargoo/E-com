package com.jzargo.media.event;

import com.jzargo.media.config.KafkaPropertyStorage;
import com.jzargo.media.storages.persistent.StorageType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaDlqEventPublisher implements DlqEventPublisher{
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaPropertyStorage kafkaPropertyStorage;

    public KafkaDlqEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, KafkaPropertyStorage kafkaPropertyStorage) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaPropertyStorage = kafkaPropertyStorage;
    }

    @Override
    public void publishNotAvailableEvent(
            StorageType currentStorageType, 
            StorageType targetStorageType, 
            String fileUri) {

        String topicName = kafkaPropertyStorage.getFailedFileOperationTopic()
                .getName()
                + "_" +
                currentStorageType.toString();

        FailedOperationEvent failedOperationEvent =
                new FailedOperationEvent(targetStorageType, fileUri);

        kafkaTemplate.send(topicName, failedOperationEvent);
    }

    @Override
    public void publishStorageRecoveryEvent(StorageType storageType) {

        String topicName =
                kafkaPropertyStorage.getStorageRecoveryTopic().getName();

        kafkaTemplate.send(
                topicName,
                new RecoveryEvent(storageType)
        );

    }
}