package com.jzargo.media.storages.primary;

import com.jzargo.media.exceptions.CannotDownloadFileException;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.protobuf.ContentType;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface MediaPrimaryStorageService {

    void deleteFile(String fileUri) throws CannotProcessException;

    DownloadedFile downloadFile(String fileUri) throws CannotDownloadFileException;

    String uploadPartOfFile(String uploadId, String key, InputStream is, Integer partNumber, Long length);


    String startUploadingFile(ContentType contentType, String key, String version) throws WrongContentTypeException;

    CompleteMultipartUploadResponse finishFileUploading(String key, String uploadId, List<String> tags) throws CannotProcessException;

    void abortMultipartFile(String key, String uploadId);

    /**
     * Uploads a file as a single stream.
     *
     * @param file contains the file content, content type, and URI.
     *
     * @param ttl specifies how long the data should be retained in storage.
     *            If empty, the storage must not enable TTL for the file.
     *
     * @return the version ID of the saved file. If no version is provided,
     *         the storage must generate one.
     */
    String uploadFullFile(DownloadedFile file, Optional<String> ttl);

    Boolean existsByVersion(String uri, String version) throws NoSuchKeyException;
}