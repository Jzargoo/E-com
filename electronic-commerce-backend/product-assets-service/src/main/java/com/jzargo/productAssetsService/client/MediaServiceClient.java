package com.jzargo.productAssetsService.client;

import com.jzargo.productAssetsService.exception.CannotAddMediaFileException;
import com.jzargo.productAssetsService.model.PlainFile;
import com.jzargo.protobuf.MediaFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MediaServiceClient {

    String sendFile(PlainFile file) throws CannotAddMediaFileException;

    Flux<MediaFile> receiveFile(Mono<String> mediaUri);

    String changeFile(PlainFile plainFile, Integer version, String prevUri);
}
