package com.jzargo.media.storages.virtual;

import com.jzargo.media.event.FileCreatedSyncEvent;
import com.jzargo.media.event.FileRequestEvent;
import com.jzargo.media.storages.persistent.StorageType;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;

@Profile("kafka")
@KafkaListener
public class KafkaVirtualStorageProcessor implements VirtualStorageProcessor {

    private final StorageType storageType;

    public KafkaVirtualStorageProcessor(StorageType storageType) {
        this.storageType = storageType;
    }


    @Override
    public StorageType getType() {
        return storageType;
    }

    @Override
    @KafkaHandler
    public void processFileRequestEvent(FileRequestEvent event) {

    }

    @Override
    @KafkaHandler
    public void processFileCreatedSyncEvent(FileCreatedSyncEvent event) {

    }
}
