package com.jzargo.media.storages.virtual;

import com.jzargo.media.config.balancing.MediaPersistentStorageBackendRegistry;
import com.jzargo.media.event.DlqPublisher;
import com.jzargo.media.event.EventPublisher;
import com.jzargo.media.event.FileCreatedSyncEvent;
import com.jzargo.media.event.FileRequestEvent;
import com.jzargo.media.exceptions.CannotDownloadFileException;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.model.DownloadedFile;
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

    private final EventPublisher eventPublisher;

    private final DlqPublisher dlqPublisher;

    private final MediaPersistentStorageBackendRegistry registry;

    public KafkaVirtualStorageProcessor(
            String consumerGroupId,
            StorageType storageType, MediaPrimaryStorageService primaryStorageService, EventPublisher eventPublisher, DlqPublisher dlqPublisher,
            MediaPersistentStorageBackendRegistry registry) {

        this.consumerGroupId = consumerGroupId;
        this.storageType = storageType;
        this.primaryStorageService = primaryStorageService;
        this.eventPublisher = eventPublisher;
        this.dlqPublisher = dlqPublisher;
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

        try {
            var downloadedFile = primaryStorageService.downloadFile(event.getFileURL());


            mediaBackend.storeFile(downloadedFile);

            eventPublisher.publishFileCreatedSyncEvent(
                    new FileCreatedSyncEvent(
                            storageType, event.getFileURL()
                    )
            );

            primaryStorageService.deleteFile(event.getFileURL());

        } catch (CannotDownloadFileException e) {
            log.debug("CannotDownloadFileException, there might be a problem so file cannot be downloaded; therefore, we will treat like nothing happened", e);
        }
    }



    @Override
    public void processFileCreatedSyncEvent(FileCreatedSyncEvent event) throws CannotProcessException {
        MediaPersistentStorageBackend myBackend =
                registry.getBackendByStorageType(storageType);


        if (myBackend.existsByURL(event.getFileURL())) {
            logFileExist(event.getFileURL());
            return;
        }

        MediaPersistentStorageBackend backendByStorageType =
                registry.getBackendByStorageType(event.getStorageType());

        if  (backendByStorageType == null) {
            logUnavailableService(event.getFileURL());
            throw new CannotProcessException();
        }

        DownloadedFile file = backendByStorageType.getFile(event.getFileURL());

        myBackend.storeFile(file);

        eventPublisher.publishFileCreatedSyncEvent(
                new FileCreatedSyncEvent(
                        storageType, event.getFileURL()
                )
        );
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
            // We move offset because it is uncoverable error.
            // We cannot process it e.g. unavailable service.
            // We just reserve it in a special topic

            dlqPublisher.reserveUnprocessedEventUnavailableService(event);
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
        try {
            processFileRequestEvent(event);
            acknowledgment.acknowledge();
        } catch (CannotProcessException e) {
            log.error(e.getMessage());
        }
    }

    private void logUnavailableService(String fileURL){
        log.trace(
                "The kafka virtual storage processor for storage type {} received a request {}; " +
                "however, it cannot process because service is not active", storageType, fileURL
        );
    }
}
