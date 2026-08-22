package com.jzargo.media.service;

import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.event.EventPublisher;
import com.jzargo.media.event.FileRequestEvent;
import com.jzargo.media.exceptions.CannotDownloadFileException;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.FileNotFoundException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.media.storages.persistent.MediaPersistentStorageBackend;
import com.jzargo.media.storages.primary.MediaPrimaryStorageService;
import com.jzargo.protobuf.MediaFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MediaStorageServiceImpl implements MediaStorageService {

    private final MediaPrimaryStorageService mediaPrimaryStorageService;
    private final EventPublisher eventPublisher;

    private final MediaPersistentStorageBackend mediaPersistentStorageBackend;
    private final ApplicationPropertyStorage applicationPropertyStorage;

    public MediaStorageServiceImpl(MediaPrimaryStorageService mediaPrimaryStorageService, EventPublisher eventPublisher, MediaPersistentStorageBackend mediaPersistentStorageBackend, ApplicationPropertyStorage applicationPropertyStorage) {
        this.mediaPrimaryStorageService = mediaPrimaryStorageService;
        this.eventPublisher = eventPublisher;
        this.mediaPersistentStorageBackend = mediaPersistentStorageBackend;
        this.applicationPropertyStorage = applicationPropertyStorage;
    }

    @Override
    public String storeChunkFile(String key, String uploadId, InputStream is, Integer partNumber, Long length) {

        return mediaPrimaryStorageService.uploadPartOfFile(
                uploadId, key, is, partNumber, length
        );

    }

    @Override
    public String initiateFile(MediaFile mediaFile, String key, String version) throws WrongContentTypeException {

        return mediaPrimaryStorageService.startUploadingFile(
                mediaFile.getContentType(), key, version
        );

    }

    @Override
    public void finishFileUploading(String key, String uploadId, List<String> tags, boolean isVideo) throws CannotProcessException {

        CompleteMultipartUploadResponse completeMultipartUploadResponse =
                mediaPrimaryStorageService.finishFileUploading(key, uploadId, tags);

        eventPublisher.publishFileRequestedEvent(
                new FileRequestEvent(key)
        );

        if (isVideo) {
            try {

                DownloadedFile downloadedFile = mediaPrimaryStorageService.downloadFile(
                        completeMultipartUploadResponse.key()
                );

                generatePosterAndPublishIt(downloadedFile);

            } catch (CannotDownloadFileException ignored) {}
        }

    }

    @Override
    public void abortMultipartFile(String key, String uploadId) {
        mediaPrimaryStorageService.abortMultipartFile(key, uploadId);
    }

    @Override
    public DownloadedFile getFileStream(String uri) throws CannotProcessException, FileNotFoundException, WrongContentTypeException {
        try {

            return
                    mediaPrimaryStorageService
                            .downloadFile(uri);

        } catch (CannotDownloadFileException e) {
            // We want to download from secondary storage to primary and try again
            // If we cannot download  from secondary => file DNE. Throw exception invalid uri

            synchronized (uri.intern()) {

                // Other threads will not create download and put a file into minio.
                // Instead, they will return primary storage because first thread put into primary storage
                // It only can seem to be useless like "why we try access it twice", but the meaning is that
                // first thread put object into storage
                try {
                    return mediaPrimaryStorageService
                            .downloadFile(uri);
                } catch (CannotDownloadFileException ignored) {}

                if (!mediaPersistentStorageBackend.existsByURI(uri)) {
                    throw new FileNotFoundException();
                }

                DownloadedFile file = mediaPersistentStorageBackend.getFile(uri);

                String ttl = applicationPropertyStorage.getAws().getObjectTtl();


                mediaPrimaryStorageService.uploadFullFile(file, Optional.ofNullable(ttl));

                return file;
            }

        }
    }

    @Override
    public void storeFullFile(DownloadedFile file) {

        mediaPrimaryStorageService.uploadFullFile(
                file, Optional.empty()
        );

    }

    @Override
    public Boolean existsByVersion(String uri, String version) {

        try {

            return mediaPrimaryStorageService.existsByVersion(uri, version);

        } catch (NoSuchKeyException e) {

            log.error("The key is missing in primary storage. Looking in secondary!");

            try {

                return mediaPersistentStorageBackend.existsByVersionedURI(uri, version);

            } catch (CannotProcessException ex) {
               return false;
            }

        }
    }

    @Async("poster-executor")
    protected void generatePosterAndPublishIt(
            DownloadedFile fileVideo
    ){
        try {
            DownloadedFile posterFromVideo =
                    MediaHelper.getPosterFromVideo(fileVideo.getContent(), fileVideo.getContentType());

            posterFromVideo.setFileUri(
                    fileVideo.getFileUri()
                            .replace(
                                    "." + MediaHelper.getMediaPostfix(
                                            fileVideo.getContentType()
                                    ),
                                    "." + MediaHelper.getMediaPostfix(
                                            posterFromVideo.getContentType()
                                    )
                            )
            );

            mediaPrimaryStorageService.uploadFullFile(fileVideo, Optional.empty());

            eventPublisher.publishFileRequestedEvent(
                    new FileRequestEvent(posterFromVideo.getFileUri())
            );

        } catch (IOException | CannotProcessException | WrongContentTypeException e) {
            throw new RuntimeException(e);
        }

    }
}
