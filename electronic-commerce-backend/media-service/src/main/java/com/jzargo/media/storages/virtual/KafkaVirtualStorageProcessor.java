package com.jzargo.media.storages.virtual;

import com.jzargo.media.config.balancing.MediaPersistentStorageBackendRegistry;
import com.jzargo.media.event.DlqEventPublisher;
import com.jzargo.media.event.EventPublisher;
import com.jzargo.media.event.FileCreatedSyncEvent;
import com.jzargo.media.event.FileRequestEvent;
import com.jzargo.media.exceptions.CannotDownloadFileException;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
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

    private final DlqEventPublisher dlqEventPublisher;

    private final MediaPrimaryStorageService primaryStorageService;

    private final EventPublisher eventPublisher;


    private final MediaPersistentStorageBackendRegistry registry;

    public KafkaVirtualStorageProcessor(
            String consumerGroupId,
            StorageType storageType, DlqEventPublisher dlqEventPublisher, MediaPrimaryStorageService primaryStorageService, EventPublisher eventPublisher,
            MediaPersistentStorageBackendRegistry registry) {

        this.consumerGroupId = consumerGroupId;
        this.storageType = storageType;
        this.dlqEventPublisher = dlqEventPublisher;
        this.primaryStorageService = primaryStorageService;
        this.eventPublisher = eventPublisher;
        this.registry = registry;
    }


    @Override
    public StorageType getStorageType() {
        return storageType;
    }


    private void logUnavailableService(String fileURL){
        log.trace(
                "The kafka virtual storage processor for storage type {} received a request {}; " +
                        "however, it cannot process because service is not active", storageType, fileURL
        );
    }


    private void logFileExist(String fileURL){
        log.trace("File {} already exists", fileURL);
    }



    @KafkaListener(
            topics = {
                    "#{kafkaPropertyStorage.fileTransferTopic.name}",
            },
            groupId = "#{__listener.consumerGroupId}",
            properties = {"spring.kafka.enable.auto.commit=false"},
            containerFactory = "manualAckFactory"
    )
    public void handleFileRequestEvent(FileRequestEvent event, Acknowledgment acknowledgment) {

        try {
            processFileRequestEvent(event);

            acknowledgment.acknowledge();

        } catch (CannotProcessException e) {
            log.error(e.getMessage());
        }

    }

    @Override
    public void processFileRequestEvent(FileRequestEvent event) throws CannotProcessException {

        MediaPersistentStorageBackend mediaBackend =
                registry.getBackendByStorageType(storageType);

        if  (mediaBackend == null) {
            logUnavailableService(event.getFileURL());
            throw new CannotProcessException();
        }

        if (mediaBackend.existsByURI(event.getFileURL())) {
            logFileExist(event.getFileURL());
            return;
        }

        try {
            var downloadedFile = primaryStorageService.downloadFile(event.getFileURL());


            String fileUri = mediaBackend.storeFile(downloadedFile);

            eventPublisher.publishFileCreatedSyncEvent(
                    new FileCreatedSyncEvent(
                            storageType, fileUri
                    )
            );

            primaryStorageService.deleteFile(event.getFileURL());

        } catch (CannotDownloadFileException e) {
            log.debug("CannotDownloadFileException, there might be a problem so file cannot be downloaded; therefore, we will treat like nothing happened", e);
        }
    }




    @KafkaListener(
            topics = {
                    "#{kafkaPropertyStorage.fileSyncTopic.name}"
            },
            groupId = "#{__listener.consumerGroupId}",
            properties = {"spring.kafka.consumer.enable.auto.commit=false"},
            containerFactory = "manualAckFactory"

    )
    public void handleFileCreatedSyncEvent(FileCreatedSyncEvent event, Acknowledgment acknowledgment) {

        try {
            processFileCreatedSyncEvent(event);
            acknowledgment.acknowledge();
        } catch (CannotProcessException e) {
            log.error(e.getMessage());

            // We do not move offset because it is uncoverable error.
            // We cannot process it
        }
    }

    /**
     * Processes the {@link FileCreatedSyncEvent} synchronization event, enabling P2P replication
     * of a file across independent storage backends.
     * <p>
     * <b>Execution Flow:</b>
     * <ol>
     *   <li><b>Duplicate Check:</b> If the file at the given URI already exists in the current storage ({@code myBackend}),
     *       processing terminates immediately to prevent redundant downloads.</li>
     *   <li><b>Source Availability Check:</b> Requests the source backend matching the storage type specified in the event.
     *       <ul>
     *         <li>If the source backend is unavailable (null), the file URI is pushed to a local
     *             DLQ ({@code myBackend.addIntoDlqFile}). The file will be processed later when the remote
     *             storage emits a recovery event.</li>
     *       </ul>
     *   </li>
     *   <li><b>Download & Storage:</b> Downloads the file from the source backend and persists it to {@code myBackend}.</li>
     *   <li><b>Cluster Notification:</b> Upon successful storage, publishes a new synchronization event
     *       announcing that the current storage now owns a replica of the file.</li>
     * </ol>
     *
     * <b>Handling Corrupted Files (SWCF — Storage With Corrupted File):</b>
     * <br>
     * Receiving a {@link WrongContentTypeException} indicates that the file is corrupted on the source backend.
     * The recovery logic addresses two scenarios:
     * <ul>
     *   <li><b>Scenario A (Sole Owner):</b> If the source was the only owner of the file, the copy is lost permanently.</li>
     *   <li><b>Scenario B (Multiple Owners):</b> If other storages possess a valid file, their events will guaranteed
     *       have a higher offset in Kafka. Once another storage successfully fetches and publishes a valid replica,
     *       the SWCF will consume this new event and re-attempt the download.</li>
     * </ul>
     *
     * @param event synchronization event containing the source storage type and file URI
     * @throws CannotProcessException if a fatal error occurs during event processing
     */
    @Override
    public void processFileCreatedSyncEvent(FileCreatedSyncEvent event) throws CannotProcessException {
        MediaPersistentStorageBackend myBackend =
                registry.getBackendByStorageType(storageType);

        if   (myBackend == null) {
            throw new CannotProcessException();
        }


        if (myBackend.existsByURI(event.getFileUri())) {
            logFileExist(event.getFileUri());
            return;
        }

        MediaPersistentStorageBackend backendByStorageType =
                registry.getBackendByStorageType( event.getStorageType() );

        if  (backendByStorageType == null) {
            logUnavailableService(event.getFileUri());

            dlqEventPublisher.publishNotAvailableEvent(
                    storageType, event.getStorageType(), event.getFileUri()
            );

            return;
        }


        try {
            DownloadedFile file = backendByStorageType.getFile(event.getFileUri());

            String uri = myBackend.storeFile(file);

            eventPublisher.publishFileCreatedSyncEvent(
                    new FileCreatedSyncEvent(
                            storageType, uri
                    )
            );

        } catch (WrongContentTypeException e) {
            log.error("Tried to download a file from a secondary storage {} but it was corrupted",event.getStorageType(), e);
        }
    }


}
