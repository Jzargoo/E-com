package com.jzargo.media.storages.primary;

import com.jzargo.media.exceptions.CannotDownloadFileException;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.protobuf.ContentType;

public interface MediaPrimaryStorageService {
    void deleteFile(String fileUri) throws CannotProcessException;

    DownloadedFile downloadFile(String fileUri) throws CannotDownloadFileException;

    void uploadPartOfFile(String uploadId, String key, byte[] bytes);

    String startUploadingFile(ContentType contentType, String key) throws WrongContentTypeException;

    void abortMultipartFile(String key, String uploadId);
}
