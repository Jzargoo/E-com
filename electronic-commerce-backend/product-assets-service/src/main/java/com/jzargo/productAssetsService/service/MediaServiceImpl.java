package com.jzargo.productAssetsService.service;


import com.jzargo.productAssetsService.client.MediaServiceClient;
import com.jzargo.productAssetsService.driver.FallbackMediaDriver;
import com.jzargo.productAssetsService.entity.FallbackMediaContent;
import com.jzargo.productAssetsService.entity.MediaContent;
import com.jzargo.productAssetsService.entity.ProductAssets;
import com.jzargo.productAssetsService.exception.*;
import com.jzargo.productAssetsService.helper.ContentTypeParser;
import com.jzargo.productAssetsService.mapper.MediaContentCreateMapper;
import com.jzargo.productAssetsService.model.PlainFile;
import com.jzargo.productAssetsService.repository.FallbackMediaContentRepository;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import com.jzargo.productAssetsService.repository.ProductAssetsRepository;
import com.jzargo.protobuf.ContentType;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Slf4j
@Service
@Qualifier("internalMediaService")
@Transactional(readOnly = true) // if not provided, data must be in immutable state
public class MediaServiceImpl implements MediaService {

    private final MediaServiceClient mediaServiceClient;
    private final FallbackMediaContentRepository fallbackMediaContentRepository;
    private final FallbackMediaDriver fallbackMediaDriver;
    private final MediaContentCreateMapper mediaContentCreateMapper;
    private final ProductAssetsRepository productAssetsRepository;
    private final MediaService mediaService;
    private final MediaContentRepository mediaContentRepository;

    public MediaServiceImpl(
            MediaServiceClient mediaServiceClient,
            FallbackMediaContentRepository fallbackMediaContentRepository,
            FallbackMediaDriver fallbackMediaDriver,
            MediaContentCreateMapper mediaContentCreateMapper, ProductAssetsRepository productAssetsRepository, MediaService mediaService, MediaContentRepository mediaContentRepository) {

        this.mediaServiceClient = mediaServiceClient;
        this.fallbackMediaContentRepository = fallbackMediaContentRepository;
        this.fallbackMediaDriver = fallbackMediaDriver;
        this.mediaContentCreateMapper = mediaContentCreateMapper;
        this.productAssetsRepository = productAssetsRepository;
        this.mediaService = mediaService;
        this.mediaContentRepository = mediaContentRepository;
    }


    @Override
    @Transactional
    @CircuitBreaker(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    @Bulkhead(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    public void addMediaContent(MultipartFile mediaContent, Long productId, Integer shopId)
            throws ProductNotFoundException, ShopDoesNotOwnProductException, CannotAddMediaFileException, UnsupportedContentType {

        ProductAssets product = productAssetsRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        try {

            ContentType parse = ContentTypeParser.parse(
                    Objects
                            .requireNonNull(
                                    mediaContent.getContentType()
                            ).trim()
            );

            String key = getUniqueUriByProductIdAndContentType(productId, parse);

            String uri = mediaServiceClient.sendFile(
                new PlainFile (
                        mediaContent.getInputStream(),
                        parse,
                        key
                )
            );

            product.addMediaContent(
                    mediaContentCreateMapper.map(uri)
            );

            productAssetsRepository.save(
                    product
            );


        } catch (IOException e) {
            log.error(
                    "addMediaContent error: {}",
                    e.getMessage(), e
            );
        }

    }

    @SuppressWarnings("unused")
    public void fallbackAddingMediaContent(MultipartFile mediaContent, Long productId, Integer shopId)
            throws ProductNotFoundException, ShopDoesNotOwnProductException, UnsupportedContentType, IOException {

        log.debug("Fallback method that adds media content was invoked");

        ProductAssets product = productAssetsRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        ContentType parse = ContentTypeParser.parse(
                Objects
                        .requireNonNull(
                                mediaContent.getContentType()
                        ).trim()
        );


        String key = getUniqueUriByProductIdAndContentType(productId, parse);


        FallbackMediaContent build = FallbackMediaContent.builder()

                .contentType(
                        ContentTypeParser.parse(Objects.requireNonNull(mediaContent.getContentType()))
                )


                .mediaUri(key)

                .build();

        build.setProduct(product);


        fallbackMediaDriver.saveFile(mediaContent.getInputStream());

        fallbackMediaContentRepository.save(build);
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "mediaService", fallbackMethod = "fallbackAddingAvatar")
    @Bulkhead(name = "mediaService", fallbackMethod = "fallbackAddingAvatar")
    public void addAvatar(MultipartFile image, Long productId, Integer shopId)
            throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException, UnsupportedContentType {

        ProductAssets product = productAssetsRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        ContentType parse = ContentTypeParser.parse(
                Objects
                        .requireNonNull(
                                image.getContentType()
                        ).trim()
        );


        MediaContent avatar = productAssetsRepository
                .findById(productId)
                .map(ProductAssets::getAvatar)
                .orElseThrow(ProductNotFoundException::new);

        String imageUri = mediaServiceClient.changeFile(

                new PlainFile(
                        image.getInputStream(),
                        parse,
                        getUniqueUriByProductIdAndContentType(productId, parse)
                ),

                (avatar.getMediaVersion()),

                avatar.getUri()
        );

        product.setAvatar(
                mediaContentCreateMapper.map(imageUri)
        );

        productAssetsRepository.save(product);
    }

    @SuppressWarnings("unused")
    public void fallbackAddingAvatar(MultipartFile image, Long productId, Integer shopId)
            throws ShopDoesNotOwnProductException, ProductNotFoundException, IOException, UnsupportedContentType {
        log.debug("Fallback method for adding avatar was invoked");

        ProductAssets product = productAssetsRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        FallbackMediaContent content = FallbackMediaContent.builder()
                .isAvatar(true)
                .contentType(
                        ContentTypeParser.parseImage(
                                Objects.requireNonNull(image.getContentType())
                        )
                )
                .build();

        content.setProduct(product);

        fallbackMediaDriver.saveFile(image.getInputStream());

        fallbackMediaContentRepository.save(content);
    }

    @Override
    public PlainFile getAvatar(Long productId)
        throws ProductNotFoundException {

        MediaContent avatar = productAssetsRepository
                .findById(productId)
                .map(ProductAssets::getAvatar)
                .orElseThrow(ProductNotFoundException::new);



        return mediaServiceClient.receiveFile(avatar.getUri());
    }

    @Override
    public PlainFile getMediaContent(Long assetId)
        throws AssetNotFoundException {

        MediaContent mediaContent = mediaContentRepository
                .findById(assetId)
                .orElseThrow(AssetNotFoundException::new);

        log.trace("Got media content with id {}, sending a request to mediaClient", assetId);

        return mediaServiceClient.receiveFile(mediaContent.getUri());
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

    public List<Long> findIdsByProductId(Long productId) {

        return productAssetsRepository
                .findById(productId)
                .orElseThrow()
                .getMediaContents()
                .stream()
                .map(MediaContent::getId)
                .toList();
    }
}