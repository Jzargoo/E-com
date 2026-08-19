package com.jzargo.productAssetsService.client;

import com.jzargo.productAssetsService.exception.CannotAddMediaFileException;
import com.jzargo.protobuf.MediaFile;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface MediaServiceClient {

    Mono<String> sendFile(String key, Flux<DataBuffer> data) throws CannotAddMediaFileException;

    Flux<MediaFile> receiveFile(Mono<String> mediaUri);

    Mono<String> changeFile(Flux<DataBuffer> content, String key, Integer version, String prevUri);
}
