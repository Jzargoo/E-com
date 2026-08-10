package com.jzargo.productAssetsService.client;

import com.google.protobuf.ByteString;
import com.jzargo.productAssetsService.config.ApplicationPropertyStorage;
import com.jzargo.productAssetsService.exception.CannotAddMediaFileException;
import com.jzargo.productAssetsService.model.PlainFile;
import com.jzargo.protobuf.MediaContentURI;
import com.jzargo.protobuf.MediaFile;
import com.jzargo.protobuf.MediaServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
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

        int size = 0;

        try {

            byte[] read = file.getIs().readNBytes(portionSize);

            while (read.length != 0) {

                streamObserver.onNext(
                        MediaFile.newBuilder()
                                .setContentType(file.getContentType())
                                .setUri(file.getUri())
                                .setContentChunk(
                                        ByteString.copyFrom(read)
                                )
                                .build()
                );

                size += read.length;

                read = file.getIs().readNBytes(portionSize);
            }

        } catch (IOException e) {

            log.error(
                    "Occurred an exception during sending a file chunk. Sent {} bytes successfully",
                    size,e
            );

            throw new CannotAddMediaFileException(e);
        }


        streamObserver.onCompleted();


        var minSpeedSending = 12 * 1024;

        long timeout = (size / minSpeedSending) + 30;

        try {

            return future.get(timeout, TimeUnit.SECONDS);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {

            throw new CannotAddMediaFileException(e);

        }

    }

    @Override
    public PlainFile receiveFile(String mediaId) {

        var plainFile = new PlainFile();

        mediaServiceStub.getMediaContent(

                MediaContentURI.newBuilder()
                        .setMediaURI(mediaId)
                        .build(),

                new StreamObserver<>() {

                    @Override
                    public void onNext(MediaFile mediaFile) {

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onCompleted() {

                    }

                }
        );

        return null;
    }

    @Override
    public String changeFile(PlainFile plainFile, Integer version, String prevUri) {
        return "";
    }

}