package com.jzargo.media.storages.virtual;

import com.jzargo.media.event.FileCreatedSyncEvent;
import com.jzargo.media.event.FileRequestEvent;
import com.jzargo.media.storages.persistent.StorageType;

public interface VirtualStorageProcessor {
    StorageType getType();

    void processFileRequestEvent(FileRequestEvent event);
    void processFileCreatedSyncEvent(FileCreatedSyncEvent event);
}
