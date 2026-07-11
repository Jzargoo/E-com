package com.jzargo.media;

import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.media.storages.persistent.MediaPersistentStorageBackend;
import com.jzargo.media.storages.persistent.StorageType;

public class StubAzureCloudPersistentMediaBackend implements MediaPersistentStorageBackend {
    @Override
    public String storeFile(DownloadedFile file) throws CannotProcessException {
        return "";
    }

    @Override
    public String replaceFile(DownloadedFile file, String previousFileUri) throws CannotProcessException {
        return "";
    }

    @Override
    public void deleteFile(String fileUri) throws CannotProcessException {

    }

    @Override
    public StorageType getStorageType() {
        return StorageType.CLOUD_AZURE;
    }

    @Override
    public boolean existsByURI(String fileUri) throws CannotProcessException {
        return false;
    }

    @Override
    public DownloadedFile getFile(String fileUri) throws CannotProcessException, WrongContentTypeException {
        return null;
    }
}
