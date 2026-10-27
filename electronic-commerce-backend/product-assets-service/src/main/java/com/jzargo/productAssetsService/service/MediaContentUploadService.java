package com.jzargo.productAssetsService.service;

import com.jzargo.productAssetsService.client.MediaServiceClient;
import com.jzargo.productAssetsService.entity.MediaContent;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import com.jzargo.protobuf.ContentType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// This service works with media content to separate into separate transaction.
//
@Service
@RequiredArgsConstructor
public class MediaContentUploadService {

    private final MediaContentRepository mediaContentRepository;
    private final MediaServiceClient mediaServiceClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<MediaUploadResult> saveAndUpload(
            MediaContent mediaContent,
            Flux<DataBuffer> content,
            ContentType contentType
    ) {
        return mediaContentRepository.save(mediaContent)
                .flatMap(saved ->
                        mediaServiceClient.sendFile(
                                saved.getUri(),
                                content,
                                contentType
                        ).map(versionedUri ->
                                new MediaUploadResult(
                                        saved.getId(),
                                        saved.getProductAssetsId(),
                                        versionedUri.getVersion()
                                )
                        )
                );
    }

    public record MediaUploadResult(
            Long id,
            Long productAssetsId,
            String version
    ) {}
}