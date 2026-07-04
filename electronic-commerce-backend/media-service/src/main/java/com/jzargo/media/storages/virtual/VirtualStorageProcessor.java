package com.jzargo.media.storages.virtual;

import com.jzargo.media.event.FileCreatedSyncEvent;
import com.jzargo.media.event.FileRequestEvent;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.storages.persistent.StorageType;

public interface VirtualStorageProcessor {
    StorageType getStorageType();

    void processFileRequestEvent(FileRequestEvent event)
            throws CannotProcessException;
    void processFileCreatedSyncEvent(FileCreatedSyncEvent event)
            throws CannotProcessException;
}
