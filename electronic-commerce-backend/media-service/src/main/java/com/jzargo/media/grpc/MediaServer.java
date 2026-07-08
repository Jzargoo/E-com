package com.jzargo.media.grpc;

import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.service.MediaStorageService;
import com.jzargo.protobuf.ChangeMediaFile;
import com.jzargo.protobuf.MediaContentURI;
import com.jzargo.protobuf.MediaFile;
import com.jzargo.protobuf.MediaServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@GrpcService
public class MediaServer extends MediaServiceGrpc.MediaServiceImplBase {

    private final MediaStorageService mediaStorageService;

    public MediaServer(MediaStorageService mediaStorageService) {
        super();
        this.mediaStorageService = mediaStorageService;
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

        return new StreamObserver<MediaFile>() {
            @Override
            public void onNext(MediaFile mediaFile) {
                if  (isFirst.get()) {
                    try {
                        mediaStorageService.initiateFile(mediaFile);
                    } catch (IOException | WrongContentTypeException e) {
                        throw new RuntimeException(e);
                    }
                    isFirst.set(false);
                }

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        };
    }
}
