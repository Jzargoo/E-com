package com.jzargo.productAssetsService.client;

import com.jzargo.productAssetsService.driver.FallbackMediaDriver;
import com.jzargo.productAssetsService.entity.Avatar;
import com.jzargo.productAssetsService.entity.FallbackMediaContent;
import com.jzargo.productAssetsService.entity.MediaContent;
import com.jzargo.productAssetsService.exception.CannotAddMediaFileException;
import com.jzargo.productAssetsService.exception.TaskCompletedException;
import com.jzargo.productAssetsService.repository.AvatarRepository;
import com.jzargo.productAssetsService.repository.FallbackMediaContentRepository;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import com.jzargo.protobuf.VersionedURI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class MediaServiceFallbackTaskAndManager {

    private final FallbackMediaContentRepository fallbackMediaContentRepository;
    private final MediaServiceClient mediaServiceClient;
    private final FallbackMediaDriver fallbackMediaDriver;
    private final MediaContentRepository mediaContentRepository;
    private final AvatarRepository avatarRepository;

    public MediaServiceFallbackTaskAndManager(
            FallbackMediaContentRepository fallbackMediaContentRepository,
            MediaServiceClient mediaServiceClient,
            FallbackMediaDriver fallbackMediaDriver,
            MediaContentRepository mediaContentRepository,
            AvatarRepository avatarRepository) {

        this.fallbackMediaContentRepository = fallbackMediaContentRepository;
        this.mediaServiceClient = mediaServiceClient;
        this.fallbackMediaDriver = fallbackMediaDriver;
        this.mediaContentRepository = mediaContentRepository;
        this.avatarRepository = avatarRepository;
    }

    @SneakyThrows
    @Transactional
    public Mono<Void> task() {
        return takeFreeContent()
                .flatMap(this::uploadAndFinalize)
                .then();
    }

    private Mono<FallbackMediaContent> takeFreeContent() {

        return fallbackMediaContentRepository.findFirstByIsFreeIsTrue()

                .switchIfEmpty(Mono.error(TaskCompletedException::new))

                .flatMap(this::lockContent);

    }

    private Mono<FallbackMediaContent> lockContent(FallbackMediaContent content) {

        return fallbackMediaContentRepository

                .lockProcessing(content.getQueueId())

                .flatMap(updatedRows -> {

                    if (updatedRows == 0) {
                        return Mono.error(
                                new IllegalStateException(
                                        "Could not lock fallback media content: " + content.getQueueId()
                                )
                        );
                    }

                    content.setIsFree(false);

                    return Mono.just(content);

                });

    }

    private Mono<FallbackMediaContent> uploadAndFinalize(FallbackMediaContent content)  {

        return upload(content)

                .flatMap(version -> saveMediaContent(content, version))

                .flatMap(savedContent -> finalizeContent(content, savedContent))

                .onErrorResume(error -> {

                    log.error(
                            "Error processing media {}",
                            content.getMediaUri(),
                            error
                    );

                    content.setIsFree(true);

                    return Mono.error(error);

                })

                .thenReturn(content);
    }

    private Mono<String> upload(FallbackMediaContent content) {

        Flux<DataBuffer> fileStream =
                fallbackMediaDriver.getFile(content.getMediaUri());

        if (content.getPreviousMediaVersion() == null) {

            return mediaServiceClient
                    .sendFile(
                            content.getMediaUri(),
                            fileStream,
                            content.getContentType()
                    )
                    .map(VersionedURI::getVersion)
                    .onErrorMap(
                            error -> new CannotAddMediaFileException(
                                    "Could not add media file for uri %s"
                                            .formatted(content.getMediaUri())
                            )
                    );

        }

        return mediaServiceClient

                .changeFile(
                        fileStream,
                        content.getMediaUri(),
                        content.getPreviousMediaVersion(),
                        content.getPreviousUri()
                )

                .map(VersionedURI::getVersion);

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<MediaContent> saveMediaContent(
            FallbackMediaContent content,
            String version
    ) {
        return getOrCreateMediaContent(content, version)
                .flatMap(mediaContentRepository::save)
                .flatMap(saved -> updateAvatarIfNeeded(content, saved));
    }

    private Mono<MediaContent> getOrCreateMediaContent(
            FallbackMediaContent content,
            String version
    ) {
        if (content.getPreviousMediaVersion() == null) {
            return Mono.just(
                    MediaContent.builder()
                            .uri(content.getMediaUri())
                            .mediaVersion(version)
                            .productId(content.getProductId())
                            .build()
            );
        }

        return mediaContentRepository
                .findByUri(content.getPreviousUri())
                .switchIfEmpty(
                        Mono.error(
                                new IllegalStateException(
                                        "Media content not found: " + content.getPreviousUri()
                                )
                        )
                )
                .map(existing -> {
                    existing.setMediaVersion(version);
                    existing.setUri(content.getMediaUri());
                    return existing;
                });
    }

    private Mono<MediaContent> updateAvatarIfNeeded(
            FallbackMediaContent content,
            MediaContent saved
    ) {
        if (!Boolean.TRUE.equals(content.getIsAvatar())) {
            return Mono.just(saved);
        }

        return avatarRepository
                .findById(content.getProductId())
                .switchIfEmpty(
                        Mono.just(
                                new Avatar(
                                        saved.getProductId(),
                                        saved.getId()
                                )
                        )
                )
                .flatMap(avatarRepository::save)
                .thenReturn(saved);
    }

    private Mono<MediaContent> finalizeContent(
            FallbackMediaContent queueItem,
            MediaContent savedContent
    ) {

        return Mono.fromRunnable(
                        () -> fallbackMediaDriver.deleteFile(savedContent.getUri())
                )
                .then(
                        fallbackMediaContentRepository
                                .deleteById(queueItem.getQueueId())
                )
                .thenReturn(savedContent);

    }

}