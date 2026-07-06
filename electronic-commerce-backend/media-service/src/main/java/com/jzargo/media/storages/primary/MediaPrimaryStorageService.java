package com.jzargo.media.storages.primary;

import com.jzargo.media.exceptions.CannotDownloadFileException;
import com.jzargo.media.model.DownloadedFile;

public interface MediaPrimaryStorageService {
    void deleteFile(String fileUri);

    DownloadedFile downloadFile(String fileUri) throws CannotDownloadFileException;

    void uploadFile();
}
