package com.jzargo.productAssetsService.helper;

import com.google.protobuf.ByteString;
import com.jzargo.productAssetsService.model.PlainFile;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.MediaFile;
import io.netty.buffer.ByteBufAllocator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Component
public class PlainFileConverter {

    private final DataBufferFactory dataBufferFactory =
            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

    public PlainFile convertFromFlux(Flux<MediaFile> mediaFileFlux) {
        Flux<MediaFile> shared = mediaFileFlux.share();

        Mono<ContentType> map = shared
                .next()
                .map(MediaFile::getContentType);

        Flux<DataBuffer> buffer = mediaFileFlux
                .map(MediaFile::getContentChunk)
                .map(ByteString::toByteArray)
                .map(dataBufferFactory::wrap);

        return PlainFile.builder()
                .upload(buffer)
                .contentType(map)
                .build();
    }
}
