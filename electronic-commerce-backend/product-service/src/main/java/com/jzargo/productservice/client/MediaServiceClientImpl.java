package com.jzargo.productservice.client;

import com.google.protobuf.ByteString;
import com.jzargo.productservice.config.ApplicationPropertyStorage;
import com.jzargo.productservice.exception.CannotAddMediaFileException;
import com.jzargo.productservice.model.PlainFile;
import com.jzargo.protobuf.MediaContentURI;
import com.jzargo.protobuf.MediaFile;
import com.jzargo.protobuf.MediaServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class MediaServiceClientImpl implements MediaServiceClient {


    private final MediaServiceGrpc.MediaServiceStub mediaServiceStub;
    private final ApplicationPropertyStorage applicationPropertyStorage;

    public MediaServiceClientImpl(MediaServiceGrpc.MediaServiceStub mediaServiceStub, ApplicationPropertyStorage applicationPropertyStorage) {
        this.mediaServiceStub = mediaServiceStub;
        this.applicationPropertyStorage = applicationPropertyStorage;
    }


    @Override
    public String sendFile(PlainFile file) throws CannotAddMediaFileException{

        final String[] uri = new String[1];

        CompletableFuture<String> future = new CompletableFuture<>();

        var streamObserver = mediaServiceStub.addMediaFile(
                new StreamObserver<>() {
                    @Override
                    public void onNext(MediaContentURI mediaContentURI) {
                        uri[0] = mediaContentURI.getMediaURI();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        log.error(throwable.getMessage(), throwable);

                        future.completeExceptionally( new CannotAddMediaFileException(throwable) );
                    }

                    @Override
                    public void onCompleted() {
                        log.info(
                                "Sending media a media file was completed successfully for uri {}", uri[0]
                        );

                        future.complete(uri[0]);
                    }
                }
        );

        Integer portionSize = applicationPropertyStorage.getGrpc().getPortionSize();

        Long remSize = file.getLength();

        while (remSize != 0){

            var contentChunkLength = Math.min(remSize,portionSize);


            try {

                streamObserver.onNext(
                        MediaFile.newBuilder()
                                .setContentChunk(
                                        ByteString.copyFrom(
                                                file.getContent().readNBytes( (int) contentChunkLength )
                                        )
                                )
                                .setUri(file.getUri())
                                .setContentType(file.getContentType())
                                .build()
                );

            } catch (IOException e) {

                streamObserver.onError(e);

                log.error("Failed to read or send file chunks", e);

                throw new CannotAddMediaFileException();

            }

            remSize -= contentChunkLength;
        }

        streamObserver.onCompleted();


        var minSpeedSending = 12 * 1024;

        long timeout = (file.getLength() / minSpeedSending) + 30;

        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {

            throw new CannotAddMediaFileException(e);

        }

    }

    @Override
    public List<PlainFile> receiveFiles(List<String> mediaIds) {
        return List.of();
    }

    @Override
    public PlainFile receiveFile(String mediaIds) {
        return null;
    }

}