package com.jzargo.media.storages.virtual;

import com.jzargo.media.config.balancing.MediaPersistentStorageBackendRegistry;
import com.jzargo.media.event.EventPublisher;
import com.jzargo.media.event.FileCreatedSyncEvent;
import com.jzargo.media.event.FileRequestEvent;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.storages.persistent.MediaPersistentStorageBackend;
import com.jzargo.media.storages.persistent.StorageType;
import com.jzargo.media.storages.primary.MediaPrimaryStorageService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

@Slf4j
public class KafkaVirtualStorageProcessor implements VirtualStorageProcessor {

    @Getter
    private final String consumerGroupId;

    private final StorageType storageType;

    private final MediaPrimaryStorageService primaryStorageService;

    private EventPublisher eventPublisher;

    private final MediaPersistentStorageBackendRegistry registry;

    public KafkaVirtualStorageProcessor(
            String consumerGroupId,
            StorageType storageType, MediaPrimaryStorageService primaryStorageService,
            MediaPersistentStorageBackendRegistry registry) {

        this.consumerGroupId = consumerGroupId;
        this.storageType = storageType;
        this.primaryStorageService = primaryStorageService;
        this.registry = registry;
    }

    @Override
    public StorageType getStorageType() {
        return storageType;
    }

    @Override
    public void processFileRequestEvent(FileRequestEvent event) throws CannotProcessException {
        MediaPersistentStorageBackend mediaBackend =
                registry.getBackendByStorageType(storageType);

        if  (mediaBackend == null) {
            logUnavailableService(event.getFileURL());
            throw new CannotProcessException();
        }

        if (mediaBackend.existsByURL(event.getFileURL())) {
            logFileExist(event.getFileURL());
            return;
        }

        primaryStorageService.downloadFile();

        try {
            primaryStorageService.deleteFile();
        } catch (Exception e) {}
    }

    @Override
    public void processFileCreatedSyncEvent(FileCreatedSyncEvent event) throws CannotProcessException {
        MediaPersistentStorageBackend mediaBackend =
                registry.getBackendByStorageType(storageType);

        if  (mediaBackend == null) {
            logUnavailableService(event.getFileURL());
            throw new CannotProcessException();
        }

        if (mediaBackend.existsByURL(event.getFileURL())) {
            logFileExist(event.getFileURL());
            return;
        }

        MediaPersistentStorageBackend backendByStorageType =
                registry.getBackendByStorageType(storageType);

        if (backendByStorageType == null) {
            logUnavailableService(event.getFileURL());


        }

    }

    private void logDeprecatedType(String storageType) {
        log.warn("Deprecated storage type: {}", storageType );
    }

    @KafkaListener(
            topics = {
                    "#{kafkaPropertyStorage.fileSyncTopic.name}"
            },
            groupId = "#{__listener.consumerGroupId}"

    )
    public void handleFileCreatedSyncEvent(FileCreatedSyncEvent event, Acknowledgment acknowledgment) {

        try {
            processFileCreatedSyncEvent(event);
            acknowledgment.acknowledge();
        } catch (CannotProcessException e) {
            log.error(e.getMessage());
        }
    }

    private void logFileExist(String fileURL){
        log.trace("File {} already exists", fileURL);
    }

    @KafkaListener(
            topics = {
                    "#{kafkaPropertyStorage.fileTransferTopic.name}",
            },
            groupId = "#{__listener.consumerGroupId}"
    )
    public void handleFileRequestEvent(FileRequestEvent event, Acknowledgment acknowledgment) {
        MediaPersistentStorageBackend mediaBackend =
                registry.getBackendByStorageType(storageType);


        mediaPrimary
    }

    private void logUnavailableService(String fileURL){
        log.trace(
                "The kafka virtual storage processor for storage type {} received a request; " +
                "however, it cannot process because service is not active", storageType
        );
    }
}
