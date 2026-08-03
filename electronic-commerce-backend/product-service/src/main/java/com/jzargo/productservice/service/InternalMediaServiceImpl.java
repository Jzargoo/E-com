package com.jzargo.productservice.service;


import com.jzargo.productservice.client.MediaServiceClient;
import com.jzargo.productservice.driver.FallbackMediaDriver;
import com.jzargo.productservice.entity.FallbackMediaContent;
import com.jzargo.productservice.entity.MediaContent;
import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.exception.UnsupportedContentType;
import com.jzargo.productservice.helper.ContentTypeParser;
import com.jzargo.productservice.model.PlainFile;
import com.jzargo.productservice.repository.FallbackMediaContentRepository;
import com.jzargo.productservice.repository.ProductRepository;
import com.jzargo.protobuf.ContentType;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.jzargo.productservice.helper.ContentTypeParser.parse;
import static com.jzargo.productservice.helper.ContentTypeParser.parseImage;

@Slf4j
@Service
@Qualifier("internalMediaService")
@Transactional(readOnly = true) // if not provided, data must be in immutable state
public class InternalMediaServiceImpl implements MediaService {

    private final MediaServiceClient mediaServiceClient;
    private final ProductRepository productRepository;
    private final FallbackMediaContentRepository fallbackMediaContentRepository;
    private final FallbackMediaDriver fallbackMediaDriver;

    public InternalMediaServiceImpl(MediaServiceClient mediaServiceClient, ProductRepository productRepository, FallbackMediaContentRepository fallbackMediaContentRepository, FallbackMediaDriver fallbackMediaDriver) {
        this.mediaServiceClient = mediaServiceClient;
        this.productRepository = productRepository;
        this.fallbackMediaContentRepository = fallbackMediaContentRepository;
        this.fallbackMediaDriver = fallbackMediaDriver;
    }


    @Override
    @Transactional
    @CircuitBreaker(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    @Bulkhead(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    public void addMediaContent(MultipartFile mediaContent, Long productId, Integer shopId)
            throws ProductNotFoundException, ShopDoesNotOwnProductException, UnsupportedContentType {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        try {

            ContentType parse = parse(
                    Objects
                            .requireNonNull(
                                    mediaContent.getContentType()
                            ).trim()
            );

            String key = getUniqueUriByProductIdAndContentType(productId, parse);

            String uri = mediaServiceClient.sendFile(
                new PlainFile(
                        mediaContent.getInputStream(),
                        parse,
                        mediaContent.getSize(),
                        key
                )
            );

            product.addMedia(uri);

            productRepository.save(product);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @SuppressWarnings("unused")
    public void fallbackAddingMediaContent(MultipartFile mediaContent, Long productId, Integer shopId)
            throws ProductNotFoundException, ShopDoesNotOwnProductException, UnsupportedContentType, IOException {

        log.debug("Fallback method that adds media content was invoked");

        Product product = productRepository.findById(productId).orElseThrow(
                ProductNotFoundException::new
        );

        if (product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        ContentType parse = parse(
                Objects
                        .requireNonNull(
                                mediaContent.getContentType()
                        ).trim()
        );


        String key = getUniqueUriByProductIdAndContentType(productId, parse);

        FallbackMediaContent build = FallbackMediaContent.builder()
                .contentType(
                        parse(Objects.requireNonNull(mediaContent.getContentType()))
                )
                .length(mediaContent.getSize())
                .mediaUri(
                    key
                )
                .build();

        build.setProduct(product);


        fallbackMediaDriver.saveFile(mediaContent.getInputStream(), mediaContent.getSize());

        fallbackMediaContentRepository.save(build);
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "mediaService", fallbackMethod = "fallbackAddingAvatar")
    @Bulkhead(name = "mediaService", fallbackMethod = "fallbackAddingAvatar")
    public void addAvatar(MultipartFile image, Long productId, Integer shopId)
            throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        ContentType parse = parse(
                Objects
                        .requireNonNull(
                                image.getContentType()
                        ).trim()
        );


        MediaContent avatar = productRepository
                .findById(productId)
                .map(Product::getMediaContent)
                .orElseThrow(ProductNotFoundException::new)
                .stream()
                .filter(m -> !m.isAvatar())
                .findAny()
                .orElseThrow(ProductNotFoundException::new);

        String imageName = mediaServiceClient.changeFile(
                new PlainFile(
                        image.getInputStream(),
                        parse,
                        image.getSize(),
                        getUniqueUriByProductIdAndContentType(productId, parse)
                ),
                (avatar.getMediaVersion()),
                avatar.getId().getMediaContentUri()
        );

        product.setAvatar(imageName);

        productRepository.save(product);
    }

    @SuppressWarnings("unused")
    public void fallbackAddingAvatar(MultipartFile image, Long productId, Integer shopId, Long size)
            throws ShopDoesNotOwnProductException, ProductNotFoundException, IOException {
        log.debug("Fallback method for adding avatar was invoked");

        Product product = productRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        FallbackMediaContent content = FallbackMediaContent.builder()
                .isAvatar(true)
                .contentType(
                        parseImage(
                                Objects.requireNonNull(image.getContentType())
                        )
                )
                .length(size)
                .build();

        content.setProduct(product);

        fallbackMediaDriver.saveFile(image.getInputStream(), size);

        fallbackMediaContentRepository.save(content);
    }

    @Override
    public PlainFile getAvatar(Long productId)
        throws ProductNotFoundException, IOException {

        MediaContent avatar = productRepository
                .findById(productId)
                .map(Product::getMediaContent)
                .orElseThrow(ProductNotFoundException::new)
                .stream()
                .filter(m -> !m.isAvatar())
                .findAny()
                .orElseThrow(ProductNotFoundException::new);



        return mediaServiceClient.receiveFile(avatar.getId().getMediaContentUri());
    }

    @Override
    public List<PlainFile> getMediaContent(Long productId)
        throws ProductNotFoundException {

        List<MediaContent> allImages = productRepository
                .findById(productId)
                .map(Product::getMediaContent)
                .orElseThrow(
                        ProductNotFoundException::new
                );

        List<PlainFile> mediaContents = new ArrayList<>();

        for (MediaContent mediaContent : allImages) {

            PlainFile plainFile = mediaServiceClient.receiveFile(
                    mediaContent.getId()
                            .getMediaContentUri()
            );

            mediaContents.add(plainFile);
        }

        return mediaContents;
    }

    private String getUniqueUriByProductIdAndContentType(Long productId, ContentType contentType){
        return "products/%s/%s.%s"
                .formatted(
                        productId,

                        UUID.randomUUID().toString()
                                .replace(".", ""),

                        ContentTypeParser.getMediaPostfix(contentType)
                );

    }
}