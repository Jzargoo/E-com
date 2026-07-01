package com.jzargo.media.backend;

import com.jzargo.protobuf.PlainFile;

import java.util.List;


public interface MediaPersistentStorageBackend {
    List<String> storeFiles(List<PlainFile> files);

    String storeFile(PlainFile file);

    void replaceFile(PlainFile plainFile, String id);

    void deleteFile(String id);
}
