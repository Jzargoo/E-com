package com.jzargo.media.event;

import com.jzargo.media.storages.persistent.StorageType;

public interface DlqEventPublisher {
    void publishNotAvailableEvent(StorageType currentStorageType, StorageType targetStorageType, String fileUri);

    void publishStorageRecoveryEvent(StorageType storageType);
}
