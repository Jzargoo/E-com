package com.jzargo.media.storages.persistent;

import com.jzargo.media.model.DownloadedFile;
import com.jzargo.protobuf.PlainFile;

import java.util.List;


public interface MediaPersistentStorageBackend {
    List<String> storeFiles(List<PlainFile> files);

    String storeFile(DownloadedFile file);

    void replaceFile(DownloadedFile file, String previousFileUri);

    void deleteFile(String id);

    StorageType getStorageType();

    boolean existsByURL(String fileUri);

    DownloadedFile getFile(String fileUri);
}
