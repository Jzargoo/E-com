package com.jzargo.productAssetsService.client;

import com.jzargo.productAssetsService.config.ApplicationPropertyStorage;
import com.jzargo.productAssetsService.exception.CannotAddMediaFileException;
import com.jzargo.protobuf.MediaContentURI;
import com.jzargo.protobuf.MediaFile;
import com.jzargo.protobuf.MediaServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


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
    public Mono<String> sendFile(String key, Flux<DataBuffer> data) throws CannotAddMediaFileException{

        return Mono.empty();
    }

    @Override
    public Flux<MediaFile> receiveFile(Mono<String> mediaUri) {

        return mediaUri.flatMapMany(

                uri -> Flux.create(
                        sink -> {

                            StreamObserver<MediaFile> fluxObserver = new StreamObserver<>() {

                                @Override
                                public void onNext(MediaFile val) {
                                    sink.next(val);
                                }

                                @Override
                                public void onError(Throwable t) {
                                    log.error("Error occurred in receiving a file!", t);
                                    sink.error(t);
                                }

                                @Override
                                public void onCompleted() {
                                    sink.complete();
                                }
                            };

                            mediaServiceStub.getMediaContent(
                                    MediaContentURI.newBuilder()
                                            .setMediaURI(uri)
                                            .build(),
                                    fluxObserver
                            );

                        }

                )

        );

    }

    @Override
    public Mono<String> changeFile(Flux<DataBuffer> content, String key, Integer version, String prevUri){
        return Mono.just("");
    }

}