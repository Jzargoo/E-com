package com.jzargo.productAssetsService.service;

import com.jzargo.productAssetsService.exception.AssetNotFoundException;
import com.jzargo.productAssetsService.exception.ProductNotFoundException;
import com.jzargo.productAssetsService.exception.UnsupportedContentType;
import com.jzargo.productAssetsService.model.PlainFile;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

public interface MediaService {

    Mono<Long> addMediaContent(Flux<DataBuffer> content, Long productId, Integer shopId, String contentType)
            throws UnsupportedContentType;

    Mono<Long> addAvatar(Flux<DataBuffer> content, Long productId, Integer shopId, String contentType)
            throws UnsupportedContentType;

    PlainFile getMediaContent(Long assetId)
            throws IOException, AssetNotFoundException;

    Flux<Long> findIdsByProductId(Long productId);

    PlainFile getAvatar(Long productId)
                throws IOException, ProductNotFoundException;

}
