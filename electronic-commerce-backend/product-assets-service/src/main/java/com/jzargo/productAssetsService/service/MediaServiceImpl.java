package com.jzargo.productAssetsService.service;


import com.jzargo.productAssetsService.client.MediaServiceClient;
import com.jzargo.productAssetsService.driver.FallbackMediaDriver;
import com.jzargo.productAssetsService.entity.Avatar;
import com.jzargo.productAssetsService.entity.FallbackMediaContent;
import com.jzargo.productAssetsService.entity.MediaContent;
import com.jzargo.productAssetsService.entity.ProductAssets;
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
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static com.jzargo.productAssetsService.helper.UriCreator.getUniqueUriByProductIdAndContentType;


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
    @Transactional()
    @CircuitBreaker(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    @Bulkhead(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    public Mono<Long> addMediaContent(Flux<DataBuffer> content, Long productId, Integer shopId, String contentType)
            throws UnsupportedContentType {

        Mono<ProductAssets> productAssets =
                productAssetsRepository
                        .findByProductIdAndShopId(productId, shopId)
                        .flatMap(
                                asset -> MediaServiceValidator.validateProductAssets(asset, shopId)
                        )
                        .switchIfEmpty(
                                Mono.error(
                                        ProductNotFoundException::new
                                )
                        )
                        .doOnNext(GlobalLogger::logFoundAsset);

        return productAssets

                .flatMap(

                asset -> {

                    ContentType parsed = ContentTypeParser.parse(contentType);

                    String key = getUniqueUriByProductIdAndContentType(
                            asset.getProductId(), parsed
                    );

                    MediaContent build = MediaContent.builder()
                            .productId(asset.getProductId())
                            .uri(key)
                            .build();


                    return saveAndUpload(build, key, content, parsed);

                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Mono<Integer> updateVersionByUri(String uri, String version) {
       return mediaContentRepository.updateVersion(uri, version);
    }

    @SuppressWarnings("unused")
    public Mono<Long> fallbackAddingMediaContent(
            Flux<DataBuffer> content, Long productId, Integer shopId, String contentType)
            throws UnsupportedContentType {

        GlobalLogger.logStartingExecuting("fallback adding media content");

        Mono<ProductAssets> product = productAssetsRepository
                .findById(productId)

                .flatMap(asset -> MediaServiceValidator.validateProductAssets(asset, shopId))

                .switchIfEmpty(
                        Mono.error(
                                ProductNotFoundException::new
                        )
                )

                .doOnNext(GlobalLogger::logFoundAsset);

        return product

                .flatMap(
                        asset -> {

                            ContentType parse = ContentTypeParser.parse(contentType);

                            String key =  getUniqueUriByProductIdAndContentType(productId, parse);


                            FallbackMediaContent build = FallbackMediaContent.builder()
                                    .contentType(parse)
                                    .mediaUri(key)
                                    .productId(productId)
                                    .build();

                            fallbackMediaContentRepository.save(build);

                            try {

                                return fallbackMediaDriver.saveFile(content, key)
                                        .flatMap(
                                                file -> Mono.error(CreatedInFallbackException::new)
                                        );

                            } catch (IOException e) {

                                GlobalLogger.logException(e, "saving media content in fallback");

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
                .doOnNext(GlobalLogger::logFoundAsset);

        ContentType parsed = ContentTypeParser.parseImage(contentType);

        String key = getUniqueUriByProductIdAndContentType(productId, parsed);

        return product

                .flatMap(asset -> MediaServiceValidator.validateProductAssets(asset, shopId))

                .switchIfEmpty(Mono.error(ProductNotFoundException::new))

                .flatMap(
                        asset ->
                                avatarRepository.findByProductId(
                                        asset.getProductId()
                                ).flatMap(avatar ->
                                        mediaContentRepository.findById(
                                                avatar.getContentId()
                                        )
                                )
                )

                // A: mediaContent is found
                .flatMap(mc -> {

                    String prevUri = mc.getUri();
                    mc.setUri(key);

                    return mediaContentRepository

                            .save(mc)

                            .flatMap(
                                    saved ->
                                            mediaServiceClient
                                                    .changeFile(
                                                            content, key,
                                                            saved.getMediaVersion(),
                                                            prevUri)
                            )

                            .flatMap(
                            verUri -> updateVersionByUri(verUri.getUri(), verUri.getVersion())
                            );
                })

                .flatMap(columns -> mediaContentRepository.findByUri(key))

                .mapNotNull(MediaContent::getId)

                // B: mediaContent is not found => create a representation of a file
                .switchIfEmpty(
                        Mono.defer(
                                () -> {

                                    MediaContent build = MediaContent.builder()
                                            .uri(key)
                                            .productId(productId)
                                            .build();

                                    return saveAndUpload(build, key, content, parsed);
                                }
                            )
                );
    }

    protected Mono<Long> saveAndUpload(MediaContent mc, String key, Flux<DataBuffer> content, ContentType contentType) {
        return mediaContentRepository.save(mc)
                .flatMap(
                        saved -> {
                            try {

                                return mediaServiceClient.sendFile(key, content, contentType);

                            } catch (CannotAddMediaFileException e) {
                               GlobalLogger.logException(e, "saving media content in service");

                               return Mono.error(e);
                            }
                        }
                ).flatMap(
                        versionedURI -> updateVersionByUri(versionedURI.getUri(), versionedURI.getVersion())
                ).flatMap(
                        ignored -> mediaContentRepository.findByUri(key)
                ).mapNotNull(MediaContent::getId);
    }

    @SuppressWarnings("unused")
    public Mono<Long> fallbackAddingAvatar(Flux<DataBuffer> content, Long productId, Integer shopId, String contentType)
            throws UnsupportedContentType {

        GlobalLogger.logStartingExecuting("fallback adding or changing avatar");

        Mono<ProductAssets> productAssetsMono = productAssetsRepository

                .findById(productId)

                .flatMap(asset -> MediaServiceValidator.validateProductAssets(asset, shopId))

                .switchIfEmpty(Mono.error(ProductNotFoundException::new))

                .doOnNext(GlobalLogger::logFoundAsset);

        return productAssetsMono

                .flatMap(
                        asset -> {

                            ContentType parsed = ContentTypeParser.parseImage(contentType);

                            String key =  getUniqueUriByProductIdAndContentType(asset.getProductId(), parsed);

                            return
                                    avatarRepository.findById( asset.getProductId() )

                                            .mapNotNull(Avatar::getContentId)

                                            .flatMap(mediaContentRepository::findById)

                                            .flatMap(
                                            mediaContent -> {


                                                FallbackMediaContent build = FallbackMediaContent.builder()
                                                        .isAvatar(true)
                                                        .productId(asset.getProductId())
                                                        .previousMediaVersion(mediaContent.getMediaVersion())
                                                        .mediaUri(key)
                                                        .build();

                                                return
                                                        fallbackMediaContentRepository.save(build)
                                                                .flatMap(fmc -> {

                                                                            try {

                                                                                return fallbackMediaDriver.saveFile(content, key)
                                                                                        .flatMap(
                                                                                                ignored -> Mono.error(CreatedInFallbackException::new)
                                                                                        )
                                                                                        .flatMap(ignored -> Mono.just(fmc));

                                                                            } catch (IOException e) {

                                                                                GlobalLogger.logException(e, "adding an avatar in fallback");

                                                                                return Mono.error(e);
                                                                            }
                                                                        });

                                            }
                                    )

                                    // THIS is expected never to be executed. Normal pipeline throws either ioexception or created in fallback exception.
                                    // Fallback should return such an error to warn a controller that a content is saved, but a client should wait
                                    .mapNotNull(FallbackMediaContent::getProductId)


                                    // B: MediaContentNotFound. Create as a regular media content and save
                                    .switchIfEmpty(
                                            saveFallbackNewAvatar(
                                                    content, productId, shopId, contentType
                                            )
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

        GlobalLogger.logStartingExecuting("get avatar for product id: " + productId);

        Mono<String> mediaUri =
                avatarRepository.findByProductId(productId)
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
                                GlobalLogger::logFoundMediaContent
                        )

                        .map(MediaContent::getUri);


        return plainFileConverter.convertFromFlux(
                mediaServiceClient.receiveFile(mediaUri)
        );

    }

    @Override
    public PlainFile getMediaContent(Long assetId) {

        GlobalLogger.logStartingExecuting("get media content for product id: " + assetId);

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
                        GlobalLogger::logFoundMediaContent
                )
                .map(MediaContent::getUri);


        PlainFile plainFile = plainFileConverter.convertFromFlux(
                mediaServiceClient.receiveFile(mediaUri)
        );

        plainFile
                .getUpload()
                .doOnError(thr -> GlobalLogger.logException(thr, "getting media content "));

        return plainFile;

    }


    public Flux<Long> findIdsByProductId(Long productId) {

        return mediaContentRepository
                .findAllByProductId(productId)
                .map(MediaContent::getId);

    }

}