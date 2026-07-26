package com.jzargo.media.service;

import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.MediaFile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UploadSession {

    private boolean isMultipart = false;

    private final TempFileBufferFactory.TempFileBuffer tempFileBuffer;

    private final MediaStorageService mediaStorageService;

    private final boolean isVideo;

    private final ContentType contentType;

    public UploadSession(TempFileBufferFactory.TempFileBuffer tempFileBuffer, ContentType contentType , MediaStorageService mediaStorageService, boolean isVideo, String key) {
        this.tempFileBuffer = tempFileBuffer;
        this.contentType = contentType;
        this.mediaStorageService = mediaStorageService;
        this.isVideo = isVideo;
        this.key = key;
    }

    private final List<String> tags = new ArrayList<>();

    private final String key;

    private String uploadId;

    public void process(MediaFile mediaFile) throws CannotProcessException {

        try {

            if (tempFileBuffer.addChunkRespectToThreshold(mediaFile.toByteArray())) {

                if (!isMultipart) {

                    uploadId = mediaStorageService.initiateFile(
                            mediaFile, key
                    );

                    isMultipart = true;

                }

                tags.add(
                        mediaStorageService.storeChunkFile(
                                key, uploadId,
                                tempFileBuffer.getChunk(),
                                tags.size() + 1,
                                tempFileBuffer.getSize()
                        )
                );

            }

        } catch (IOException | WrongContentTypeException e) {

            log.error(
                    "Occurred exception while a processing command was executed with message {}",
                    e.getMessage(), e);

            throw new CannotProcessException();

        }
    }

    public String complete() throws CannotProcessException {

        if (isMultipart) {

            mediaStorageService.finishFileUploading(
                    key, uploadId, tags, isVideo
            );

        } else {

            try {

                mediaStorageService.storeFullFile(
                        new DownloadedFile(
                                tempFileBuffer.getChunk(), tempFileBuffer.getSize(), key, contentType
                        )
                );

            } catch (IOException e) {

                log.error("Exception while storing full file {} ", key, e);
                throw new CannotProcessException();

            }


        }

        return key;
    }

    public void abort() throws CannotProcessException {

        try {

            tempFileBuffer.close();

            if (isMultipart) mediaStorageService.abortMultipartFile(key, uploadId);

        } catch (IOException e) {

            throw new CannotProcessException();

        }

    }

}