package com.jzargo.media.service;

import com.jzargo.media.event.EventPublisher;
import com.jzargo.media.event.FileRequestEvent;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.media.storages.primary.MediaPrimaryStorageService;
import com.jzargo.protobuf.MediaFile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class MediaStorageServiceImpl implements MediaStorageService {

    private final MediaPrimaryStorageService mediaPrimaryStorageService;
    private final EventPublisher eventPublisher;

    public MediaStorageServiceImpl(MediaPrimaryStorageService mediaPrimaryStorageService, EventPublisher eventPublisher) {
        this.mediaPrimaryStorageService = mediaPrimaryStorageService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String storeChunkFile(String key, String uploadId, byte[] byteArray) {
        return mediaPrimaryStorageService.uploadPartOfFile(uploadId, key, byteArray);
    }

    @Override
    public String initiateFile(MediaFile mediaFile, String key) throws IOException, WrongContentTypeException {
        MediaHelper.checkContentType(mediaFile);

        return mediaPrimaryStorageService.startUploadingFile(mediaFile.getContentType(), key);
    }

    @Override
    public void finishFileUploading(String key, String uploadId, List<String> tags) throws CannotProcessException {
        mediaPrimaryStorageService.finishFileUploading(key, uploadId, tags);

        eventPublisher.publishFileRequestedEvent(
                new FileRequestEvent(key)
        );
    }

    @Override
    public void abortMultipartFile(String key, String uploadId) {
        mediaPrimaryStorageService.abortMultipartFile(key, uploadId);
    }
}
