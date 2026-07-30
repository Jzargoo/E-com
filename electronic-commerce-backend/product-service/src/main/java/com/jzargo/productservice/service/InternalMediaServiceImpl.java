package com.jzargo.productservice.service;


import com.jzargo.productservice.client.MediaServiceClient;
import com.jzargo.productservice.driver.FallbackMediaDriver;
import com.jzargo.productservice.entity.FallbackMediaContent;
import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.exception.UnsupportedContentType;
import com.jzargo.productservice.helper.ContentTypeParser;
import com.jzargo.productservice.model.PlainFile;
import com.jzargo.productservice.repository.FallbackMediaContentRepository;
import com.jzargo.productservice.repository.ProductRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.jzargo.productservice.helper.ContentTypeParser.parse;
import static com.jzargo.productservice.helper.ContentTypeParser.parseImage;

@Slf4j
@Service
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

            String uri = mediaServiceClient.sendFile(
                new PlainFile(
                        mediaContent.getInputStream(),
                        ContentTypeParser.parse(
                                Objects.requireNonNull(
                                        mediaContent.getContentType()
                                )
                        ),
                        mediaContent.getSize()
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

        FallbackMediaContent build = FallbackMediaContent.builder()
                .contentType(
                        parse(Objects.requireNonNull(mediaContent.getContentType()))
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

        String imageName = mediaServiceClient.sendFile(
                new PlainFile(
                        image.getInputStream(),
                        parse(Objects.requireNonNull(image.getContentType())),
                        image.getSize()
                )
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
                .build();

        content.setProduct(product);

        fallbackMediaDriver.saveFile(image.getInputStream(), size);

        fallbackMediaContentRepository.save(content);
    }

    @Override
    public MultipartFile getAvatar(Long productId)
        throws ProductNotFoundException, IOException {

        String avatar = productRepository
                .findById(productId)
                .map(Product::getAvatar)
                .orElseThrow(ProductNotFoundException::new);

        return mediaServiceClient.receiveFile(avatar);
    }

    @Override
    public List<PlainFile> getMediaContent(Long productId)
        throws ProductNotFoundException {

        List<String> allImages = productRepository
                .findById(productId)
                .map(Product::getMediaContent)
                .orElseThrow(
                        ProductNotFoundException::new
                );

        return mediaServiceClient.receiveFiles(allImages);
    }



}