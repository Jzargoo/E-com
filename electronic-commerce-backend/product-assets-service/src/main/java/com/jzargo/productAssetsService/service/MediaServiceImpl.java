package com.jzargo.productAssetsService.service;


import com.jzargo.productAssetsService.client.MediaServiceClient;
import com.jzargo.productAssetsService.driver.FallbackMediaDriver;
import com.jzargo.productAssetsService.entity.Avatar;
import com.jzargo.productAssetsService.entity.FallbackMediaContent;
import com.jzargo.productAssetsService.entity.MediaContent;
import com.jzargo.productAssetsService.entity.ProductAssets;
import com.jzargo.productAssetsService.exception.*;
import com.jzargo.productAssetsService.helper.ContentTypeParser;
import com.jzargo.productAssetsService.helper.PlainFileConverter;
import com.jzargo.productAssetsService.model.PlainFile;
import com.jzargo.productAssetsService.repository.AvatarRepository;
import com.jzargo.productAssetsService.repository.FallbackMediaContentRepository;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import com.jzargo.productAssetsService.repository.ProductAssetsRepository;
import com.jzargo.protobuf.ContentType;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.UUID;


@Service
@Qualifier("internalMediaService")
@Transactional(readOnly = true) // if not provided, data must be in immutable state
public class MediaServiceImpl implements MediaService {

    private final MediaServiceClient mediaServiceClient;
    private final FallbackMediaContentRepository fallbackMediaContentRepository;
    private final FallbackMediaDriver fallbackMediaDriver;
    private final ProductAssetsRepository productAssetsRepository;
    private final MediaContentRepository mediaContentRepository;
    private final PlainFileConverter plainFileConverter;
    private final AvatarRepository avatarRepository;

    public MediaServiceImpl(
            MediaServiceClient mediaServiceClient,
            FallbackMediaContentRepository fallbackMediaContentRepository,
            FallbackMediaDriver fallbackMediaDriver,
            ProductAssetsRepository productAssetsRepository,
            MediaContentRepository mediaContentRepository, PlainFileConverter plainFileConverter,
            AvatarRepository avatarRepository) {

        this.mediaServiceClient = mediaServiceClient;
        this.fallbackMediaContentRepository = fallbackMediaContentRepository;
        this.fallbackMediaDriver = fallbackMediaDriver;
        this.productAssetsRepository = productAssetsRepository;
        this.mediaContentRepository = mediaContentRepository;
        this.plainFileConverter = plainFileConverter;
        this.avatarRepository = avatarRepository;
    }


    @Override
    @Transactional
    @CircuitBreaker(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    @Bulkhead(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    public Mono<Long> addMediaContent(Flux<DataBuffer> content, Long productId, Integer shopId, String contentType)
            throws UnsupportedContentType {

        Mono<ProductAssets> productAssets =
                productAssetsRepository
                        .findByProductIdAndShopId(productId, shopId)
                        .flatMap(
                                asset -> {
                                    if (!asset.getShopId().equals(shopId)) {

                                        MediaServiceLogger.logShopDoesNotOwn(shopId, productId);

                                        return Mono.error(ShopDoesNotOwnProductException::new);
                                    }

                                    return Mono.just(asset);
                                }
                        )
                        .doOnNext(MediaServiceLogger::logFoundAsset);

        return productAssets.flatMap(
                asset -> {
                    try {

                        ContentType parsed = ContentTypeParser.parse(contentType);

                        String key = getUniqueUriByProductIdAndContentType(
                                asset.getProductId(), parsed

                        );

                        Mono<MediaContent> save = mediaContentRepository.save(
                                MediaContent.builder()
                                        .productId(asset.getProductId())
                                        .uri(key)
                                        .build()
                        );

                        mediaServiceClient.sendFile(key, content, parsed);

                        return save.map(MediaContent::getId);

                    } catch (CannotAddMediaFileException e) {

                        MediaServiceLogger.logException(e, "adding media content");

                        return Mono.error(e);
                    }

                })

                .switchIfEmpty(
                        Mono.error(
                                ProductNotFoundException::new
                        )
                );
    }

    @SuppressWarnings("unused")
    public Mono<Long> fallbackAddingMediaContent(
            Flux<DataBuffer> content, Long productId, Integer shopId, String contentType)
            throws UnsupportedContentType {

        MediaServiceLogger.logStartingExecuting("fallback adding media content");

        Mono<ProductAssets> product = productAssetsRepository.findById(
                productId
        ).doOnNext(MediaServiceLogger::logFoundAsset);

        return product

                .flatMap(
                        asset -> {
                            if (!asset.getShopId().equals(shopId)) {

                                return Mono.error(ShopDoesNotOwnProductException::new);
                            } else {
                                return Mono.just(asset);
                            }
                        })

                .switchIfEmpty(
                        Mono.error(ProductNotFoundException::new)
                )

                .flatMap(
                        asset -> {

                            ContentType parse = ContentTypeParser.parse(contentType);

                            String key =  getUniqueUriByProductIdAndContentType(productId, parse);

                            FallbackMediaContent build = FallbackMediaContent.builder()
                                    .contentType(parse)
                                    .mediaUri(key)
                                    .mediaVersion(1)
                                    .productId(productId)
                                    .build();

                            fallbackMediaContentRepository.save(build);

                            try {

                                return fallbackMediaDriver.saveFile(content, key)
                                        .flatMap(
                                                file -> Mono.error(CreatedInFallbackException::new)
                                        );

                            } catch (IOException e) {

                                MediaServiceLogger.logException(e, "saving media content in fallback");

                                return Mono.error(e);
                            }

                        });
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "mediaService", fallbackMethod = "fallbackAddingAvatar")
    @Bulkhead(name = "mediaService", fallbackMethod = "fallbackAddingAvatar")
    public Mono<Long> addAvatar(Flux<DataBuffer> content, Long productId, Integer shopId, String contentType)
            throws UnsupportedContentType {

        Mono<ProductAssets> product = productAssetsRepository
                .findById(productId)
                .doOnNext(MediaServiceLogger::logFoundAsset);

        return product

                .flatMap(asset -> {

                    if (!asset.getShopId().equals(shopId)) {

                        MediaServiceLogger.logShopDoesNotOwn(shopId, productId);

                        return Mono.error(ShopDoesNotOwnProductException::new);
                    }

                    return Mono.just(asset);

                })

                .switchIfEmpty(Mono.error(ProductNotFoundException::new))

                .flatMap(asset -> {
                    ContentType parsed = ContentTypeParser.parseImage(contentType);
                    String key = getUniqueUriByProductIdAndContentType(productId, parsed);

                    return avatarRepository.findById(asset.getProductId())
                            .map(Avatar::getContentId)
                            .flatMap(mediaContentRepository::findById)

                            // A: MediaContent is found
                            .flatMap(mc -> {
                                var newVersion = mc.getMediaVersion() + 1;

                                var updatedMedia = MediaContent.builder()
                                        .uri(key)
                                        .id(mc.getId())
                                        .mediaVersion(newVersion)
                                        .productId(asset.getProductId())
                                        .build();

                                return mediaContentRepository.save(updatedMedia)

                                        .mapNotNull(MediaContent::getId)

                                        .flatMap(id ->
                                                mediaServiceClient.changeFile(
                                                        content, key,
                                                        mc.getMediaVersion(), mc.getUri()
                                                ).thenReturn(id)
                                        );
                            })

                            // B: mediaContent is not found => create a representation of a file
                            .switchIfEmpty(

                                    Mono.defer(
                                            () -> {

                                                MediaContent build = MediaContent.builder()
                                                        .uri(key)
                                                        .productId(asset.getProductId())
                                                        .build();

                                                return mediaContentRepository.save(build)
                                                        .flatMap(mc -> addMediaContent(content, productId, shopId, contentType));
                                            }
                                    )
                            );
                });
    }

    @SuppressWarnings("unused")
    public Mono<Long> fallbackAddingAvatar(Flux<DataBuffer> content, Long productId, Integer shopId, String contentType)
            throws UnsupportedContentType {

        MediaServiceLogger.logStartingExecuting("fallback adding or changing avatar");

        return productAssetsRepository
                .findById(productId)
                .switchIfEmpty(Mono.error(ProductNotFoundException::new))
                .doOnNext(MediaServiceLogger::logFoundAsset)
                .flatMap(
                        productAssets -> {
                            if (!productAssets.getShopId().equals(shopId)) {

                                MediaServiceLogger.logShopDoesNotOwn(shopId, productId);

                                return Mono.error(ShopDoesNotOwnProductException::new);
                            }

                            return Mono.just(productAssets);
                        }
                )
                .flatMap(
                        asset -> {

                            ContentType parsed = ContentTypeParser.parseImage(contentType);

                            String key =  getUniqueUriByProductIdAndContentType(asset.getProductId(), parsed);

                            return avatarRepository.findById(asset.getProductId())
                                    .mapNotNull(Avatar::getContentId)
                                    .flatMap(mediaContentRepository::findById)
                                    .flatMap(
                                            mediaContent -> {

                                                Integer mediaVersion = mediaContent.getMediaVersion() + 1;

                                                FallbackMediaContent build = FallbackMediaContent.builder()
                                                        .isAvatar(true)
                                                        .productId(asset.getProductId())
                                                        .mediaVersion(mediaVersion)
                                                        .mediaUri(key)
                                                        .build();

                                                return
                                                        fallbackMediaContentRepository.save(build)
                                                                .flatMap(fmc ->
                                                                        {
                                                                            try {
                                                                                return fallbackMediaDriver.saveFile(content, key)
                                                                                        .flatMap(
                                                                                                ignored -> Mono.error(CreatedInFallbackException::new)
                                                                                        )
                                                                                        .flatMap(ignored -> Mono.just(fmc));
                                                                            } catch (IOException e) {

                                                                                MediaServiceLogger.logException(e, "adding an avatar in fallback");

                                                                                return Mono.error(e);
                                                                            }
                                                                        }
                                                                );
                                            }
                                    )

                                    // THIS is expected never to be executed. Normal pipeline throws either ioexception or created in fallback exception.
                                    // Fallback should return such an error to warn a controller that a content is saved, but a client should wait
                                    .mapNotNull(FallbackMediaContent::getProductId)


                                    // B: MediaContentNotFound. Create as a regular media content and save
                                    .switchIfEmpty(
                                            saveFallbackNewAvatar(
                                                    content, productId, shopId, contentType)
                                    );
                        }
                );


    }

    private Mono<Long> saveFallbackNewAvatar(Flux<DataBuffer> content, Long productId, Integer shopId, String contentType) {
        return fallbackAddingMediaContent(content, productId, shopId, contentType)
                .flatMap(
                        val -> avatarRepository.save(
                                new Avatar(productId, val)
                        )
                )
                .mapNotNull(Avatar::getContentId);
    }

    @Override
    public PlainFile getAvatar(Long productId) {

        MediaServiceLogger.logStartingExecuting("get avatar for product id: " + productId);

        Mono<String> mediaUri =
                avatarRepository.findById(productId)
                        .map(Avatar::getContentId)

                        .switchIfEmpty(
                                Mono.error(new AssetNotFoundException(
                                        "Avatar for product id %s is not found"
                                                .formatted(productId)
                                        )
                                )
                        )

                        .flatMap(mediaContentRepository::findById)
                        .doOnNext(
                                MediaServiceLogger::logFoundMediaContent
                        )
                        .map(MediaContent::getUri);


        return plainFileConverter.convertFromFlux(
                mediaServiceClient.receiveFile(mediaUri)
        );

    }

    @Override
    public PlainFile getMediaContent(Long assetId) {

        MediaServiceLogger.logStartingExecuting("get media content for product id: " + assetId);

        Mono<String> mediaUri = mediaContentRepository
                .findById(assetId)
                .switchIfEmpty(
                        Mono.error(
                                new AssetNotFoundException(
                                        "Asset with id %s is not found".formatted(assetId)
                                )
                        )
                )
                .doOnNext(
                        MediaServiceLogger::logFoundMediaContent
                )
                .map(MediaContent::getUri);


        PlainFile plainFile = plainFileConverter.convertFromFlux(
                mediaServiceClient.receiveFile(mediaUri)
        );

        plainFile
                .getUpload()
                .doOnError(thr -> MediaServiceLogger.logException(thr, "getting media content "));

        return plainFile;
    }

    private String getUniqueUriByProductIdAndContentType(Long productId, ContentType contentType)
            throws UnsupportedContentType {

        return "products/%s/%s.%s"
                .formatted(
                        productId,

                        UUID.randomUUID().toString()
                                .replace(".", ""),

                        ContentTypeParser.getMediaPostfix(contentType)
                );

    }

    public Flux<Long> findIdsByProductId(Long productId) {

        return mediaContentRepository
                .findAllByProductId(productId)
                .map(MediaContent::getId);
    }

}