package com.jzargo.media.unitTests;


import com.jzargo.media.StubAzureCloudPersistentMediaBackend;
import com.jzargo.media.config.balancing.MediaPersistentStorageBackendRegistry;
import com.jzargo.media.event.*;
import com.jzargo.media.exceptions.CannotDownloadFileException;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.media.storages.persistent.MediaPersistentStorageBackendNative;
import com.jzargo.media.storages.persistent.StorageType;
import com.jzargo.media.storages.primary.MediaPrimaryStorageServiceS3;
import com.jzargo.media.storages.virtual.KafkaVirtualStorageProcessor;
import com.jzargo.protobuf.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.utils.async.InputStreamSubscriber;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class KafkaVirtualStorageProcessorUnitTest {
    private static String FILE_URI = "1.png";

    private DownloadedFile file =
            DownloadedFile.builder()
                    .fileUri(FILE_URI)
                    .contentType(ContentType.PNG)
                    .content(new InputStreamSubscriber())
                    .build();

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    public MediaPrimaryStorageServiceS3 mediaPrimaryStorageService;
    @Mock
    public StubAzureCloudPersistentMediaBackend azureCloudPersistentMediaBackend;
    @Mock
    public KafkaEventPublisher kafkaEventPublisher;
    @Mock
    public MediaPersistentStorageBackendNative mediaPersistentStorageBackendNative;
    @Mock
    public MediaPersistentStorageBackendRegistry registry;
    @Mock
    public  KafkaDlqEventPublisher kafkaDlqEventPublisher;

    KafkaVirtualStorageProcessor kafkaVirtualStorageProcessor;

    @BeforeEach
    public void setup() {
        kafkaVirtualStorageProcessor = new KafkaVirtualStorageProcessor(
                "test",
                StorageType.NATIVE_DISK,
                kafkaDlqEventPublisher,
                mediaPrimaryStorageService,
                kafkaEventPublisher,
                registry
        );

    }

    @Test
    public void processFileRequestEvent_fileAlreadyExist_successTest() throws CannotProcessException, CannotDownloadFileException {
        Mockito.when(
                registry.getBackendByStorageType(StorageType.NATIVE_DISK)
        ).thenReturn(mediaPersistentStorageBackendNative);

        Mockito.when(
                mediaPersistentStorageBackendNative.existsByURI(FILE_URI)
        ).thenReturn(true);

        kafkaVirtualStorageProcessor.processFileRequestEvent(new FileRequestEvent(FILE_URI));


        Mockito.verify(
                registry,
                Mockito.times(1)
        ).getBackendByStorageType(StorageType.NATIVE_DISK);

        Mockito.verify(
                mediaPersistentStorageBackendNative,
                Mockito.times(0)
        ).storeFile(any());

        Mockito.verify(
                mediaPrimaryStorageService,
                Mockito.times(0)
        ).downloadFile(anyString());

    }

    @Test
    public void processFileRequestEvent_fileNotExist_successTest() throws CannotProcessException, CannotDownloadFileException {
        Mockito.when(
                registry.getBackendByStorageType(StorageType.NATIVE_DISK)
        ).thenReturn(mediaPersistentStorageBackendNative);

        Mockito.when(
                mediaPersistentStorageBackendNative.existsByURI(anyString())
        ).thenReturn(false);

        Mockito.when(
                mediaPrimaryStorageService.downloadFile(anyString())
        ).thenReturn(
                file
        );

        Mockito.when(
                mediaPersistentStorageBackendNative.storeFile(file)
        ).thenReturn(FILE_URI);

        doNothing()
                .when(
                        kafkaEventPublisher
                ).publishFileCreatedSyncEvent(any());

        doNothing()
                .when(mediaPrimaryStorageService)
                        .deleteFile(anyString());

        kafkaVirtualStorageProcessor.processFileRequestEvent(new FileRequestEvent(FILE_URI));

        verify(
                registry,
                Mockito.times(1)
        ).getBackendByStorageType(StorageType.NATIVE_DISK);

        verify(
                mediaPersistentStorageBackendNative,
                Mockito.times(1)
        ).storeFile(any());

        verify(
                mediaPrimaryStorageService,
                Mockito.times(1)
        ).downloadFile(anyString());

        verify(
                kafkaEventPublisher,
                Mockito.times(1)
        ).publishFileCreatedSyncEvent(any());

        verify(
                mediaPrimaryStorageService,
                Mockito.times(1)
        ).deleteFile(anyString());
    }

    @Test
    public void processFileRequestEvent_UnavailableService_failTest() {
        Mockito.when(
                registry.getBackendByStorageType(StorageType.NATIVE_DISK)
        ).thenReturn(null);

        Assertions.assertThrows(CannotProcessException.class, () -> kafkaVirtualStorageProcessor.processFileRequestEvent(new FileRequestEvent(FILE_URI)));
    }

    @Test
    public void processFileSyncEvent_downloadFile_successTest() throws CannotProcessException, WrongContentTypeException {
        Mockito.when(
                registry.getBackendByStorageType(StorageType.NATIVE_DISK)
        ).thenReturn(mediaPersistentStorageBackendNative);

        Mockito.when(
                mediaPersistentStorageBackendNative.existsByURI(anyString())
        ).thenReturn(false);

        Mockito.when(
                registry.getBackendByStorageType(StorageType.CLOUD_AZURE)
        ).thenReturn(azureCloudPersistentMediaBackend);

        Mockito.when(
                azureCloudPersistentMediaBackend.getFile(FILE_URI)
        ).thenReturn(file);

        Mockito.when(
                mediaPersistentStorageBackendNative.storeFile(file)
        ).thenReturn(FILE_URI);

        doNothing()
                .when(kafkaEventPublisher)
                .publishFileCreatedSyncEvent(any());

        kafkaVirtualStorageProcessor.processFileCreatedSyncEvent(
                new FileCreatedSyncEvent(StorageType.CLOUD_AZURE, FILE_URI)
        );

        Mockito.verify(
                registry,
                Mockito.times(2)
        ).getBackendByStorageType(any());

        Mockito.verify(
                mediaPersistentStorageBackendNative,
                Mockito.times(1)
        ).storeFile(file);

        Mockito.verify(
                azureCloudPersistentMediaBackend,
                Mockito.times(1)
        ).getFile(FILE_URI);

        Mockito.verify(
                kafkaEventPublisher,
                Mockito.times(1)
        ).publishFileCreatedSyncEvent(any());
    }
}
