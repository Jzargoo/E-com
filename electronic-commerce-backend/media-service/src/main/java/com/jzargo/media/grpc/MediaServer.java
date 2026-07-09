package com.jzargo.media.grpc;

import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.helper.MediaHelper;
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

    @Override
    public void getMediaContent(MediaContentURI request, StreamObserver<MediaFile> responseObserver) {
        super.getMediaContent(request, responseObserver);
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

        return new StreamObserver<>() {
            @Override
            public void onNext(MediaFile mediaFile) {
                try {

                    if  (isFirst.get()) {

                        String keyValue = "%s/%s.%s"
                                .formatted(
                                        applicationPropertyStorage
                                                .getAws()
                                                .getBucketName(),
                                        UUID.randomUUID().toString(),
                                        MediaHelper.getMediaPostfix(mediaFile.getContentType())
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

                    mediaStorageService.finishFileUploading(key.get(), uploadId.get(), tags.get());
                } catch (CannotProcessException e) {
                    log.error("Error while processing request. Cannot send the residual bytes", e);
                    throw new RuntimeException(e);
                }

            }
        };
    }
}