package com.jzargo.productAssetsService.client;

import com.jzargo.productAssetsService.exception.CannotAddMediaFileException;
import com.jzargo.productAssetsService.model.PlainFile;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.MediaFile;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.crypto.Data;

public interface MediaServiceClient {

    String sendFile(String key, Flux<DataBuffer> data) throws CannotAddMediaFileException;

    Flux<MediaFile> receiveFile(Mono<String> mediaUri);

    Mono<String> changeFile(Flux<DataBuffer> content, String key, ContentType contentType, Integer version, String prevUri);
}
