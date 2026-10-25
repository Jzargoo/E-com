package com.jzargo.productAssetsService.service;


import com.jzargo.productAssetsService.client.MediaServiceClient;
import com.jzargo.productAssetsService.driver.FallbackMediaDriver;
import com.jzargo.productAssetsService.entity.Avatar;
import com.jzargo.productAssetsService.entity.FallbackMediaContent;
import com.jzargo.productAssetsService.entity.MediaContent;
import com.jzargo.productAssetsService.exception.*;
import com.jzargo.productAssetsService.helper.ContentTypeParser;
import com.jzargo.productAssetsService.helper.GlobalLogger;
import com.jzargo.productAssetsService.helper.MediaServiceValidator;
import com.jzargo.productAssetsService.helper.PlainFileConverter;
import com.jzargo.productAssetsService.model.PlainFile;
import com.jzargo.productAssetsService.repository.AvatarRepository;
import com.jzargo.productAssetsService.repository.FallbackMediaContentRepository;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import com.jzargo.productAssetsService.repository.ProductAssetsRepository;
import com.jzargo.protobuf.ContentType;
import com.jzargo.protobuf.MediaFileMetadata;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Transactional(readOnly = true) // if not provided, data must be in immutable state
public class MediaServiceImpl implements MediaService {

    private final MediaServiceClient mediaServiceClient;
    private final FallbackMediaContentRepository fallbackMediaContentRepository;
    private final FallbackMediaDriver fallbackMediaDriver;
    private final ProductAssetsRepository productAssetsRepository;
    private final MediaContentRepository mediaContentRepository;
    private final PlainFileConverter plainFileConverter;
    private final AvatarRepository avatarRepository;
    private final MediaVersionUpdater mediaVersionUpdater;

    public MediaServiceImpl(
            MediaServiceClient mediaServiceClient,
            FallbackMediaContentRepository fallbackMediaContentRepository,
            FallbackMediaDriver fallbackMediaDriver,
            ProductAssetsRepository productAssetsRepository,
            MediaContentRepository mediaContentRepository, PlainFileConverter plainFileConverter,
            AvatarRepository avatarRepository, MediaVersionUpdater mediaVersionUpdater) {

        this.mediaServiceClient = mediaServiceClient;
        this.fallbackMediaContentRepository = fallbackMediaContentRepository;
        this.fallbackMediaDriver = fallbackMediaDriver;
        this.productAssetsRepository = productAssetsRepository;
        this.mediaContentRepository = mediaContentRepository;
        this.plainFileConverter = plainFileConverter;
        this.avatarRepository = avatarRepository;
        this.mediaVersionUpdater = mediaVersionUpdater;
    }


    @Override
    @CircuitBreaker(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    @Bulkhead(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    public Mono<Long> addMediaContent(
            Flux<DataBuffer> content,
            Long productId,
            Integer shopId,
            String contentType
    ) throws UnsupportedContentType {

        return productAssetsRepository
                .findByProductIdAndShopId(productId, shopId)
                .flatMap(asset ->
                        MediaServiceValidator.validateProductAssets(asset, shopId)
                )
                .switchIfEmpty(Mono.error(ProductNotFoundException::new))
                .doOnNext(GlobalLogger::logFoundAsset)
                .flatMap(asset -> {
                    ContentType parsed = ContentTypeParser.parse(contentType);
                    String key = getUniqueUriByProductIdAndContentType(
                            asset.getId(), parsed
                    );

                    MediaContent mediaContent = MediaContent.builder()
                            .uri(key)
                            .build();

                    return saveAndUpload(mediaContent, key, content, parsed);
                });
    }


    @SuppressWarnings("unused")
    public Mono<Long> fallbackAddingMediaContent(
            Flux<DataBuffer> content,
            Long productId,
            Integer shopId,
            String contentType
    ) throws UnsupportedContentType {

        GlobalLogger.logStartingExecuting("fallback adding media content");

        return productAssetsRepository
                .findByProductIdAndShopId(productId, shopId)
                .flatMap(asset ->
                        MediaServiceValidator.validateProductAssets(asset, shopId)
                )
                .switchIfEmpty(Mono.error(ProductNotFoundException::new))
                .flatMap(asset -> {
                    ContentType parsed = ContentTypeParser.parse(contentType);
                    String key = getUniqueUriByProductIdAndContentType(
                            asset.getId(), parsed
                    );

                    FallbackMediaContent fallback = FallbackMediaContent.builder()
                            .contentType(parsed)
                            .mediaUri(key)
                            .build();

                    try {
                        return fallbackMediaDriver.saveFile(content, key)
                                .then(fallbackMediaContentRepository.save(fallback))
                                .then(Mono.error(
                                        new CreatedInFallbackException()
                                ));
                    } catch (IOException e) {
                        GlobalLogger.logException(
                                e, "saving media content in fallback"
                        );
                        return Mono.error(e);
                    }
                });
    }


    @Override
    @CircuitBreaker(name = "mediaService", fallbackMethod = "fallbackAddingAvatar")
    @Bulkhead(name = "mediaService", fallbackMethod = "fallbackAddingAvatar")
    public Mono<Long> addAvatar(
            Flux<DataBuffer> content,
            Long productId,
            Integer shopId,
            String contentType
    ) throws UnsupportedContentType {

        ContentType parsed = ContentTypeParser.parseImage(contentType);

        return productAssetsRepository
                .findByProductIdAndShopId(productId, shopId)
                .flatMap(asset ->
                        MediaServiceValidator.validateProductAssets(asset, shopId)
                )
                .switchIfEmpty(Mono.error(ProductNotFoundException::new))
                .doOnNext(GlobalLogger::logFoundAsset)
                .flatMap(asset -> {
                    Long productAssetId = asset.getId();

                    String key = getUniqueUriByProductIdAndContentType(
                            productAssetId, parsed
                    );

                    return createAndUploadAvatarContent(
                            productAssetId, key, content, parsed
                    );
                });
    }


    private Mono<Long> createAndUploadAvatarContent(
            Long productAssetId,
            String key,
            Flux<DataBuffer> content,
            ContentType contentType
    ) {

        MediaContent mediaContent = MediaContent.builder()
                .uri(key)
                .build();

        return saveAndUpload(mediaContent, key, content, contentType)
                .flatMap(contentId ->
                        avatarRepository.findById(productAssetId)
                                .flatMap(existingAvatar ->
                                        avatarRepository.save(
                                                Avatar.builder()
                                                        .productAssetsId(productAssetId)
                                                        .contentId(contentId)
                                                        .build()
                                        )
                                )
                                .switchIfEmpty(
                                        avatarRepository.save(
                                                Avatar.builder()
                                                        .productAssetsId(productAssetId)
                                                        .contentId(contentId)
                                                        .build()
                                        )
                                )
                                .thenReturn(contentId)
                );
    }


    protected Mono<Long> saveAndUpload(
            MediaContent mediaContent,
            String key,
            Flux<DataBuffer> content,
            ContentType contentType
    ) {

        return mediaContentRepository.save(mediaContent)
                .flatMap(saved -> mediaServiceClient.sendFile(
                        key, content, contentType
                ))
                .flatMap(versionedUri ->
                        mediaVersionUpdater.updateVersionByUri(
                                versionedUri.getUri(),
                                versionedUri.getVersion()
                        )
                )
                .then(mediaContentRepository.findByUri(key))
                .mapNotNull(MediaContent::getId);
    }


    @SuppressWarnings("unused")
    public Mono<Long> fallbackAddingAvatar(
            Flux<DataBuffer> content,
            Long productId,
            Integer shopId,
            String contentType
    ) throws UnsupportedContentType {

        GlobalLogger.logStartingExecuting(
                "fallback adding or changing avatar"
        );

        ContentType parsed = ContentTypeParser.parseImage(contentType);

        return productAssetsRepository
                .findByProductIdAndShopId(productId, shopId)
                .flatMap(asset ->
                        MediaServiceValidator.validateProductAssets(asset, shopId)
                )
                .switchIfEmpty(Mono.error(ProductNotFoundException::new))
                .doOnNext(GlobalLogger::logFoundAsset)
                .flatMap(asset -> {
                    Long productAssetId = asset.getId();

                    String key = getUniqueUriByProductIdAndContentType(
                            productAssetId, parsed
                    );

                    return avatarRepository.findById(productAssetId)
                            .map(Avatar::getContentId)
                            .flatMap(mediaContentRepository::findById)
                            .map(mediaContent ->
                                    Optional.ofNullable(
                                            mediaContent.getMediaVersion()
                                    )
                            )
                            .defaultIfEmpty(Optional.empty())
                            .flatMap(previousVersion ->
                                    saveAvatarToFallback(
                                            content,
                                            key,
                                            previousVersion.orElse(null)
                                    )
                            );
                });
    }


    private Mono<Long> saveAvatarToFallback(
            Flux<DataBuffer> content,
            String key,
            String previousMediaVersion
    ) {

        FallbackMediaContent fallback = FallbackMediaContent.builder()
                .isAvatar(true)
                .previousMediaVersion(previousMediaVersion)
                .mediaUri(key)
                .build();

        try {
            return fallbackMediaDriver.saveFile(content, key)
                    .then(fallbackMediaContentRepository.save(fallback))
                    .then(Mono.error(new CreatedInFallbackException()));
        } catch (IOException e) {
            GlobalLogger.logException(
                    e, "saving avatar in fallback"
            );
            return Mono.error(e);
        }
    }


    @Override
    public PlainFile getAvatar(Long productAssetId) {

        GlobalLogger.logStartingExecuting(
                "get avatar for product asset id: " + productAssetId
        );

        Mono<String> mediaUri = avatarRepository.findById(productAssetId)
                .map(Avatar::getContentId)
                .switchIfEmpty(Mono.error(
                        new AssetNotFoundException(
                                "Avatar for product asset id %s is not found"
                                        .formatted(productAssetId)
                        )
                ))
                .flatMap(mediaContentRepository::findById)
                .doOnNext(GlobalLogger::logFoundMediaContent)
                .map(MediaContent::getUri);

        AtomicReference<MediaFileMetadata> metadataHolder = new AtomicReference<>();

        return plainFileConverter.convertFromFlux(
                mediaServiceClient.receiveFile(mediaUri, metadataHolder), metadataHolder
        );

    }


    @Override
    public PlainFile getMediaContent(Long assetId) {

        GlobalLogger.logStartingExecuting(
                "get media content for asset id: " + assetId
        );

        Mono<String> mediaUri = mediaContentRepository.findById(assetId)
                .switchIfEmpty(Mono.error(
                        new AssetNotFoundException(
                                "Asset with id %s is not found".formatted(assetId)
                        )
                ))
                .doOnNext(GlobalLogger::logFoundMediaContent)
                .map(MediaContent::getUri);

        AtomicReference<MediaFileMetadata> metadataHolder = new AtomicReference<>();

        PlainFile plainFile = plainFileConverter.convertFromFlux(
                mediaServiceClient.receiveFile(mediaUri, metadataHolder),  metadataHolder
        );

        plainFile.getUpload()
                .doOnError(thr ->
                        GlobalLogger.logException(
                                thr, "getting media content"
                        )
                );

        return plainFile;
    }


    private String getUniqueUriByProductIdAndContentType(
            Long productAssetId,
            ContentType contentType
    ) throws UnsupportedContentType {

        return "products/%s/%s.%s".formatted(
                productAssetId,
                UUID.randomUUID().toString().replace(".", ""),
                ContentTypeParser.getMediaPostfix(contentType)
        );
    }


    public Flux<Long> findIdsByProductId(Long productId) {

        return productAssetsRepository
                .findByProductId(productId)
                .flatMapMany(asset ->
                        mediaContentRepository.findAllByProductAssetsId(asset.getId())
                )
                .map(MediaContent::getId);
    }
}