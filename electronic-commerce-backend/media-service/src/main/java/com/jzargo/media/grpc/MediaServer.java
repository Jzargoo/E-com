package com.jzargo.media.grpc;

import com.google.protobuf.ByteString;
import com.jzargo.grpc.metadata.MetadataConstantKeys;
import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.media.service.MediaStorageService;
import com.jzargo.media.service.TempFileBufferFactory;
import com.jzargo.media.service.UploadSession;
import com.jzargo.protobuf.*;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@GrpcService
public class MediaServer extends MediaServiceGrpc.MediaServiceImplBase {

    private final MediaStorageService mediaStorageService;
    private final ApplicationPropertyStorage applicationPropertyStorage;
    private final TempFileBufferFactory tempFileBufferFactory;

    public MediaServer(MediaStorageService mediaStorageService, ApplicationPropertyStorage applicationPropertyStorage, TempFileBufferFactory tempFileBufferFactory) {
        super();
        this.mediaStorageService = mediaStorageService;
        this.applicationPropertyStorage = applicationPropertyStorage;
        this.tempFileBufferFactory = tempFileBufferFactory;
    }

    @Override
    public void existsByUri(VersionedURI request, StreamObserver<DoesMediaContentExist> responseObserver) {

        Boolean isExists = mediaStorageService.existsByVersion(request.getUri(), request.getVersion());

        responseObserver.onNext(
                DoesMediaContentExist.newBuilder()
                        .setUri(request.getUri())
                        .setVersion(request.getVersion())
                        .setIsExists(isExists)
                        .build()
        );

    }

    // URI ->
    @Override
    public void getMediaContent(MediaContentURI request, StreamObserver<MediaFile> responseObserver) {

        ServerCallStreamObserver<MediaFile> observer = (ServerCallStreamObserver<MediaFile>) responseObserver;

        observer.disableAutoInboundFlowControl();


        log.info("Caught a request with uri {}", request.getMediaURI());

        Integer portion = applicationPropertyStorage.getPortion();



        try {

            DownloadedFile fileStream = mediaStorageService.getFileStream(request.getMediaURI());

            if (fileStream == null || fileStream.getContentLength() == 0) {

                log.warn("file stream either is null or has 0 length");

                observer.setOnReadyHandler(() -> {
                    observer.onNext(
                            MediaFile.newBuilder().build()
                    );

                    observer.onCompleted();
                });

            } else {

                try (
                        InputStream content = fileStream.getContent()
                ) {

                    AtomicLong remSize = new AtomicLong(fileStream.getContentLength());

                    long totalChunks = Math.ceilDiv(fileStream.getContentLength(), portion);

                    log.debug(
                            "Starting sending a pieces: {} of the file by {} bytes portions",
                            totalChunks,
                            portion
                    );

                    observer.setOnReadyHandler(() -> {

                        while (observer.isReady()) {

                            for (int i = 0; i < totalChunks; i++) {

                                long min = Math.min(remSize.get(), portion);

                                remSize.addAndGet(-min);

                                try {
                                    byte[] bytes = content.readNBytes((int) min);

                                    observer.onNext(
                                            MediaFile.newBuilder()
                                                    .setContentChunk(ByteString.copyFrom(bytes))
                                                    .build()
                                    );

                                    log.debug("sending a file chunk for uri {} ({}/{})",
                                            request.getMediaURI(),
                                            i, totalChunks
                                    );

                                } catch (IOException e) {

                                    log.error("Exception in SENDING a file {}",
                                            request.getMediaURI(), e
                                    );

                                    observer.onError(e);

                                }

                            }

                        }

                    });

                    responseObserver.onCompleted();

                } catch (IOException e) {

                    log.error("MediaServer getMediaContent failed", e);

                    responseObserver.onError(e);

                }

            }

        } catch (Exception e) {

            log.error("Occurred an exception while processing a file with uri {}", request.getMediaURI(), e);

            observer.onError(e);

        }

    }

    @Override
    public StreamObserver<MediaFile> changeMediaFile(StreamObserver<VersionedURI> responseObserver) {

        log.info("Creating new MediaFile stream for a changing request");

        ChangeMediaFileMetadata changeMediaFileMetadata = MetadataConstantKeys.versionContextKey.get();

        MediaFileMetadata mediaFileMetadata = MetadataConstantKeys.fileContextKey.get();

        UploadSession uploadSession = new UploadSession(
                tempFileBufferFactory.createBuffer(),

                mediaFileMetadata.getContentType(),

                mediaStorageService,

                MediaHelper.isVideo(
                        mediaFileMetadata.getContentType()
                ),

                mediaFileMetadata.getFileUri()
        );

        return new SaveFileStreamObserver(
                uploadSession, mediaFileMetadata.getContentType(),
                exception -> {
                    log.error("Exception in SAVING a file stream for a changing request {}",
                            exception.getMessage(), exception
                    );

                    responseObserver.onError(exception);

                    try {
                        uploadSession.abort();
                    } catch (CannotProcessException e) {
                       log.error("Cannot abort upload session");
                    }
                },

                versionedURI -> {
                    log.info("Changing MediaFile for a changing request {}", versionedURI);




                }
        );


    }

    @Override
    public StreamObserver<MediaFile> addMediaFile(StreamObserver<VersionedURI> responseObserver) {

        log.info("Creating new MediaFile stream for a adding media file request");

        MediaFileMetadata mediaFileMetadata = MetadataConstantKeys.fileContextKey.get();


        AtomicBoolean isFirst = new AtomicBoolean(true);

        var uploadSession = new UploadSession(
                tempFileBufferFactory.createBuffer(),

                mediaFileMetadata.getContentType(),

                mediaStorageService,

                MediaHelper.isVideo(
                        mediaFileMetadata.getContentType()
                ),

                mediaFileMetadata.getFileUri()
        );



        return new StreamObserver<>() {

            @Override
            public void onNext(MediaFile mediaFile) {

                try {

                    if (isFirst.get()) {

                        log.info("Processing First chunk");

                        MediaHelper.checkContentType(
                                mediaFile,
                                mediaFileMetadata.getContentType()
                        );

                        isFirst.set(false);

                    }

                    log.debug("Processing a chunk!");

                    uploadSession.process(mediaFile);

                } catch (Exception e) {
                    log.error("MediaServer addMediaFile failed", e);

                    try {
                        uploadSession.abort();
                    } catch (CannotProcessException ignored) {}

                    throw new RuntimeException(e);

                }

            }


            @Override
            public void onError(Throwable throwable) {

                log.error("Error while processing request", throwable);

                try {

                    uploadSession.abort();

                    responseObserver.onError(throwable);

                } catch (CannotProcessException e) {
                    log.error("Aborting an upload session finished with error!", e);
                }

            }

            @Override
            public void onCompleted() {
                log.info("New MediaFile stream has been created");

                try {

                    VersionedURI uri = uploadSession.complete();

                    responseObserver.onNext(uri);

                    responseObserver.onCompleted();

                } catch (CannotProcessException e) {

                    log.error("Error while processing request. Cannot send the residual bytes", e);

                    throw new RuntimeException(e);
                }

            }

        };

    }
}