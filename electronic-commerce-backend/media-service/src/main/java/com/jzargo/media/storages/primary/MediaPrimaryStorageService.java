package com.jzargo.media.storages.primary;

import com.jzargo.media.exceptions.CannotDownloadFileException;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.protobuf.ContentType;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;

import java.util.List;
import java.util.Optional;

public interface MediaPrimaryStorageService {
    void deleteFile(String fileUri) throws CannotProcessException;

    DownloadedFile downloadFile(String fileUri) throws CannotDownloadFileException;

    String uploadPartOfFile(String uploadId, String key, byte[] bytes);

    String startUploadingFile(ContentType contentType, String key) throws WrongContentTypeException;

    CompleteMultipartUploadResponse finishFileUploading(String key, String uploadId, List<String> tags) throws CannotProcessException;

    void abortMultipartFile(String key, String uploadId);

    void uploadFullFile(DownloadedFile file, Optional<String> ttl);
}