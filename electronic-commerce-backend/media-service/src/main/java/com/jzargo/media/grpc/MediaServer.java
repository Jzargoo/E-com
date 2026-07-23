package com.jzargo.media.grpc;

import com.google.protobuf.ByteString;
import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.FileNotFoundException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.media.service.MediaStorageService;
import com.jzargo.media.service.SmartBuffersService;
import com.jzargo.protobuf.ChangeMediaFile;
import com.jzargo.protobuf.MediaContentURI;
import com.jzargo.protobuf.MediaFile;
import com.jzargo.protobuf.MediaServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@GrpcService
public class MediaServer extends MediaServiceGrpc.MediaServiceImplBase {

    private final MediaStorageService mediaStorageService;
    private final SmartBuffersService smartBuffersService;
    private final ApplicationPropertyStorage applicationPropertyStorage;

    public MediaServer(MediaStorageService mediaStorageService, SmartBuffersService smartBuffersService, ApplicationPropertyStorage applicationPropertyStorage) {
        super();
        this.mediaStorageService = mediaStorageService;
        this.smartBuffersService = smartBuffersService;
        this.applicationPropertyStorage = applicationPropertyStorage;
    }

    // URI ->
    @Override
    public void getMediaContent(MediaContentURI request, StreamObserver<MediaFile> responseObserver) {

        log.info("Caught a request with uri {}", request.getMediaURI());

        Integer portion = applicationPropertyStorage.getPortion();

        try {
            DownloadedFile fileStream = mediaStorageService.getFileStream(request.getMediaURI());

            if (fileStream == null || fileStream.getContentLength() == 0) {

                log.warn("file stream either is null or has 0 length");

                responseObserver.onNext(MediaFile.newBuilder().build());
            } else {

                try (
                        InputStream content = fileStream.getContent()
                        ) {

                    long remSize = fileStream.getContentLength();

                    long totalChunks = Math.ceilDiv(fileStream.getContentLength(), portion);

                    log.debug(
                            "Starting sending a pieces: {} of the file by {} bytes portions",
                            totalChunks,
                            portion
                    );

                    for (int i = 0; i < totalChunks; i++) {

                        int len = Math.toIntExact(
                                Math.min(remSize, portion)
                        );

                        remSize -= len;

                        MediaFile build = MediaFile.newBuilder()
                                .setContentType(fileStream.getContentType())
                                .setContentChunk(
                                        ByteString.copyFrom(
                                                content
                                                        .readNBytes(len)
                                        )
                                )
                                .build();

                        log.trace("Sending a chunk {}, rem size {}", i, remSize);

                        responseObserver.onNext(
                                build
                        );

                    }
                }

            }

            responseObserver.onCompleted();

        } catch (CannotProcessException | FileNotFoundException | IOException | WrongContentTypeException e) {

            log.error("MediaServer getMediaContent failed", e);

            responseObserver.onError(e);

        }
    }

    @Override
    public StreamObserver<ChangeMediaFile> changeMediaFile(StreamObserver<MediaContentURI> responseObserver) {
        return super.changeMediaFile(responseObserver);
    }

    @Override
    public StreamObserver<MediaFile> addMediaFile(StreamObserver<MediaContentURI> responseObserver) {

        log.info("Creating new MediaFile stream for a request");

        AtomicBoolean isFirst = new AtomicBoolean(true);

        AtomicReference<String> uploadId = new AtomicReference<>();

        AtomicReference<String> key = new AtomicReference<>();

        AtomicReference<List<String>> tags = new AtomicReference<>(
                new ArrayList<>()
        );

        AtomicBoolean isVideo = new AtomicBoolean(false);

        return new StreamObserver<>() {
            @Override
            public void onNext(MediaFile mediaFile) {
                try {

                    if  (isFirst.get()) {

                        String keyValue = "%s/products/%s.%s"
                                .formatted(
                                        applicationPropertyStorage
                                                .getAws()
                                                .getBucketName(),
                                        UUID.randomUUID().toString(),
                                        MediaHelper.getMediaPostfix(mediaFile.getContentType())
                                );

                        isVideo.set(
                                MediaHelper.isVideo(mediaFile.getContentType())
                        );

                        String uploadIdValue = mediaStorageService.initiateFile(mediaFile, keyValue);

                        key.set(keyValue);

                        uploadId.set(uploadIdValue);

                        smartBuffersService.addBuffer(uploadIdValue);

                        isFirst.set(false);

                    }

                    String tag = smartBuffersService.addIntoBuffer(
                            key.get(),
                            uploadId.get(),
                            mediaFile.getContentChunk()
                                    .toByteArray()
                    );

                    tags.get().add(tag);

                } catch (IOException | WrongContentTypeException | CannotProcessException e) {
                        throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error while processing request", throwable);

                try {
                    mediaStorageService.abortMultipartFile(key.get(), uploadId.get());
                    smartBuffersService.finishBuffer(key.get(), uploadId.get());

                } catch (CannotProcessException ignored) {}

            }

            @Override
            public void onCompleted() {
                log.info("New MediaFile stream has been created");

                try {
                    String tag = smartBuffersService.finishBuffer(key.get(), uploadId.get());

                    tags.get().add(tag);

                    mediaStorageService.finishFileUploading(key.get(), uploadId.get(), tags.get(), isVideo.get());

                } catch (CannotProcessException e) {
                    log.error("Error while processing request. Cannot send the residual bytes", e);
                    throw new RuntimeException(e);
                }

            }
        };
    }
}