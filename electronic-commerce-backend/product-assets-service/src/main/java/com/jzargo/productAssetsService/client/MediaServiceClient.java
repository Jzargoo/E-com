package com.jzargo.productAssetsService.client;

import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.MediaFile;
import com.jzargo.protobuf.MediaFileMetadata;
import com.jzargo.protobuf.VersionedURI;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;


public interface MediaServiceClient {

    Mono<VersionedURI> sendFile(String key, Flux<DataBuffer> data, ContentType contentType);

    Flux<MediaFile> receiveFile(Mono<String> mediaUri, AtomicReference<MediaFileMetadata> metadataHolder);

    Mono<VersionedURI> changeFile(Flux<DataBuffer> content, String key, String prevVersion, String prevUri);
}
