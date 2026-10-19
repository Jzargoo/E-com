package com.jzargo.productAssetsService.helper;

import com.google.protobuf.ByteString;
import com.jzargo.productAssetsService.model.PlainFile;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.MediaFile;
import com.jzargo.protobuf.MediaFileMetadata;
import io.netty.buffer.ByteBufAllocator;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;


@Component
public class PlainFileConverter {

    private final DataBufferFactory dataBufferFactory =
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    public PlainFile convertFromFlux(Flux<MediaFile> mediaFileFlux, AtomicReference<MediaFileMetadata> metadataHolder) {

        Flux<MediaFile> shared = mediaFileFlux.publish().autoConnect(2);

        Mono<ContentType> next = shared.mapNotNull(
                mf -> metadataHolder.get()
        )
                .next()
                .mapNotNull(MediaFileMetadata::getContentType);

        Flux<DataBuffer> buffer = mediaFileFlux
                .map(MediaFile::getContentChunk)
                .map(ByteString::toByteArray)
                .map(dataBufferFactory::wrap);

        return PlainFile.builder()
                .upload(buffer)
                .contentType(next)
                .build();

    }
}
