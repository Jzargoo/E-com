package com.jzargo.media.storages.persistent;

import com.jzargo.media.exceptions.FileAlreadyExistsException;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;

public interface MediaPersistentStorageBackend {

    String storeFile(DownloadedFile file) throws CannotProcessException, FileAlreadyExistsException;

    String replaceFile(DownloadedFile file, String previousFileUri, String prevVersion) throws CannotProcessException;

    void deleteFile(String fileUri) throws CannotProcessException;

    StorageType getStorageType();

    boolean existsByVersionedURI(String fileUri, String versionId) throws CannotProcessException;

    DownloadedFile getFile(String fileUri) throws CannotProcessException, WrongContentTypeException;

    void register();

    boolean existsByURI(String uri) throws CannotProcessException ;
}
