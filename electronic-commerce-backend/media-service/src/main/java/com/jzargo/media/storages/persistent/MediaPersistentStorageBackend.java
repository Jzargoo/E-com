package com.jzargo.media.storages.persistent;

import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;

public interface MediaPersistentStorageBackend {

    String storeFile(DownloadedFile file) throws CannotProcessException;

    String replaceFile(DownloadedFile file, String previousFileUri) throws CannotProcessException;

    void deleteFile(String fileUri) throws CannotProcessException;

    StorageType getStorageType();

    boolean existsByURI(String fileUri) throws CannotProcessException;

    DownloadedFile getFile(String fileUri) throws CannotProcessException, WrongContentTypeException;
}
