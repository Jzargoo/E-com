package com.jzargo.media.config.balancing;

import com.jzargo.media.storages.persistent.MediaPersistentStorageBackend;
import com.jzargo.media.storages.persistent.StorageType;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class MediaPersistentStorageBackendRegistry {
    private final ConcurrentHashMap<StorageType, MediaPersistentStorageBackend> backends = new ConcurrentHashMap<>();


    public MediaPersistentStorageBackend getBackendByStorageType(StorageType storageType) {
        return backends.get(storageType);
    }

    public void addBackend(MediaPersistentStorageBackend backend) {
        backends.put(backend.getStorageType(), backend);
    }

    public void removeBackend(MediaPersistentStorageBackend backend) {
        backends.remove(backend.getStorageType());
    }


    public @Nullable MediaPersistentStorageBackend next() {
        return null;
    }

    public boolean isEmpty(){
        return backends.isEmpty();
    }
}
