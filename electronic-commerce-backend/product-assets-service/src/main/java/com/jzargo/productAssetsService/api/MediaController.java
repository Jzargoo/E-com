package com.jzargo.productAssetsService.api;

import com.jzargo.productAssetsService.config.ApplicationPropertyStorage;
import com.jzargo.productAssetsService.exception.AssetNotFoundException;
import com.jzargo.productAssetsService.helper.ContentTypeParser;
import com.jzargo.productAssetsService.model.PlainFile;
import com.jzargo.productAssetsService.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/products/media")
public class MediaController {

    private final MediaService mediaService;
    private final ApplicationPropertyStorage applicationPropertyStorage;

    public MediaController(MediaService mediaService,
                           ApplicationPropertyStorage applicationPropertyStorage) {
        this.mediaService = mediaService;
        this.applicationPropertyStorage = applicationPropertyStorage;
    }


    @GetMapping("/{productId}")
    public Flux<Long> getIdsByProductId(@PathVariable Long productId) {

        log.info("Caught request to get product with id {}", productId);

        return mediaService.findIdsByProductId(productId);
    }


    @GetMapping(path = "assets/{assetId}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> getAssetsByAssetId(@PathVariable Long assetId)
            throws AssetNotFoundException, IOException {

        log.info("Caught request to get asset with id {}", assetId);


        PlainFile mediaContent = mediaService.getMediaContent(assetId);

        return mediaContent

                .getContentType()

                .map(contentType -> ResponseEntity
                        .status(HttpStatus.OK)
                        .contentType(
                                MediaType.parseMediaType(
                                        ContentTypeParser.parseIntoMime(
                                                contentType
                                        )
                                )
                        )
                        .body(mediaContent.getUpload())

                );

    }

    @PutMapping("/{productId}")
    public Mono<Long> addMediaContent(
            @RequestBody Flux<DataBuffer> content,
            @PathVariable Long productId,
            Integer shopId,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType
    ) {

        Integer maxContentByteCount =
                applicationPropertyStorage.getServer().getMaxContentByteCount();

        Flux<DataBuffer> dataBufferFlux =
                DataBufferUtils.takeUntilByteCount(content, maxContentByteCount);

        return mediaService.addMediaContent(dataBufferFlux, productId, shopId, contentType);
    }

}