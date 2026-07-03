package com.jzargo.media.grpc;

import com.jzargo.media.exceptions.CorruptedMediaRequest;
import com.jzargo.media.exceptions.ErrorDuringAddingContent;
import com.jzargo.media.service.MediaStorageService;
import com.jzargo.protobuf.MediaContentPlainFiles;
import com.jzargo.protobuf.MediaContentURIs;
import com.jzargo.protobuf.MediaServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;

@Slf4j
@GrpcService
public class MediaServer extends MediaServiceGrpc.MediaServiceImplBase {

    private final MediaStorageService mediaStorageService;

    public MediaServer(MediaStorageService mediaStorageService) {
        super();
        this.mediaStorageService = mediaStorageService;
    }

    @Override
    public void addMediaContent(MediaContentPlainFiles files, StreamObserver<MediaContentURIs> ids)  {
        log.info("Adding a media content was invoked for files {}", files.getContentCount());

        if (files.getContentList().size() != files.getContentCount()) {
            ids.onError(new CorruptedMediaRequest("The count does not match with actual content size"));
        }

        try {
            List<String> contentIds = mediaStorageService.storeFiles(files.getContentList());

            ids.onNext(
                    MediaContentURIs.newBuilder().addAllMediaURIs(contentIds).build()
            );

            ids.onCompleted();

        } catch (ErrorDuringAddingContent e) {
            log.error("Error during adding content", e);

            ids.onError(
                    new ErrorDuringAddingContent(
                        "Temporal error. The message %s".formatted(e.getMessage())
                    )
            );
        }

    }
}
