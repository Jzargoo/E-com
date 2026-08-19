package com.jzargo.productAssetsService.client;

import com.jzargo.productAssetsService.driver.FallbackMediaDriver;
import com.jzargo.productAssetsService.entity.Avatar;
import com.jzargo.productAssetsService.entity.FallbackMediaContent;
import com.jzargo.productAssetsService.entity.MediaContent;
import com.jzargo.productAssetsService.exception.CannotAddMediaFileException;
import com.jzargo.productAssetsService.repository.AvatarRepository;
import com.jzargo.productAssetsService.repository.FallbackMediaContentRepository;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
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

    @Transactional
    public void task() {

        fallbackMediaContentRepository
                        .findFirstByIsFreeIsTrue()
                        .flatMap(
                                fallbackMediaContent -> fallbackMediaContentRepository.lockProcessing(
                                            fallbackMediaContent.getQueueId()
                                    ).flatMap(
                                            value -> {

                                                if (value == 0) {
                                                    return Mono.empty();
                                                }

                                                fallbackMediaContent.setIsFree(false);

                                                return Mono.just(fallbackMediaContent);
                                            }
                                    )
                        )
                        .mapNotNull(this::uploadAndFinalize);


        // TODO: implement a gRPC existsByUri and implement this branch

    }

    @SneakyThrows
    private Mono<Void> uploadAndFinalize(FallbackMediaContent lockedContent) {

        Mono<Void> uploadMono;

        Flux<DataBuffer> fileStream = fallbackMediaDriver.getFile(lockedContent.getMediaUri());

        if (lockedContent.getMediaVersion() == 1) {

            uploadMono = mediaServiceClient.sendFile(lockedContent.getMediaUri(), fileStream)
                    .onErrorMap(CannotAddMediaFileException::new)
                    .then();

        } else {

            uploadMono = mediaServiceClient.changeFile(
                    fileStream,
                    lockedContent.getMediaUri(),
                    lockedContent.getMediaVersion(),
                    lockedContent.getPreviousUri()
            ).then();

        }

        return uploadMono
                .then(Mono.defer(() -> {
                    // Successful case

                    return fallbackMediaContentRepository.deleteById(lockedContent.getQueueId())

                            .then(
                                    Mono.fromRunnable(
                                            () -> saveMediaContent(lockedContent)
                                    )
                            )

                            .then(
                                    Mono.fromRunnable(
                                            () -> fallbackMediaDriver.deleteFile(lockedContent.getMediaUri()
                                            )
                                    )
                            );

                }))

                .doOnError(e -> {
                    log.error("Error in sending uri {}", lockedContent.getMediaUri(), e);

                    lockedContent.setIsFree(true);
                }).then();
    }

    private void saveMediaContent(FallbackMediaContent lockedContent) {

        Mono<MediaContent> mc;

        if (lockedContent.getMediaVersion() != 1) {

            mc = mediaContentRepository
                    .findByUri(lockedContent.getPreviousUri())
                    .switchIfEmpty(Mono.error(new IllegalStateException()));

        } else {

            mc = Mono.just(
                    MediaContent.builder()
                            .uri(lockedContent.getMediaUri())
                            .productId(lockedContent.getProductId())
                            .build()
            );

        }

        mc.flatMap(
                mediaContentRepository::save
        ).flatMap(
                content -> {

                    if (Boolean.TRUE.equals(lockedContent.getIsAvatar())) {
                        return avatarRepository.findById(lockedContent.getProductId())
                                .switchIfEmpty(
                                        Mono.just(
                                                new Avatar(content.getProductId(), content.getId())
                                        )
                                ).flatMap(avatarRepository::save)
                                .then(Mono.just(content));
                    }

                    return Mono.just(content);
                }
        );
    }
}