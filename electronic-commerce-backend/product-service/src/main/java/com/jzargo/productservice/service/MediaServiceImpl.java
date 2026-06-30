package com.jzargo.productservice.service;


import com.jzargo.productservice.client.MediaServiceClient;
import com.jzargo.productservice.driver.FallbackMediaDriver;
import com.jzargo.productservice.driver.FallbackMediaDriverNative;
import com.jzargo.productservice.entity.ContentType;
import com.jzargo.productservice.entity.FallbackMediaContent;
import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.exception.UnsupportedContentType;
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
import java.util.UUID;
import java.util.function.IntFunction;

@Slf4j
@Service
@Transactional(readOnly = true) // if not provided, data must be in immutable state
public class MediaServiceImpl implements MediaService {

    private final MediaServiceClient mediaServiceClient;
    private final ProductRepository productRepository;
    private final FallbackMediaContentRepository fallbackMediaContentRepository;
    private final FallbackMediaDriver fallbackMediaDriver;

    public MediaServiceImpl(MediaServiceClient mediaServiceClient, ProductRepository productRepository, FallbackMediaContentRepository fallbackMediaContentRepository, FallbackMediaDriver fallbackMediaDriver) {
        this.mediaServiceClient = mediaServiceClient;
        this.productRepository = productRepository;
        this.fallbackMediaContentRepository = fallbackMediaContentRepository;
        this.fallbackMediaDriver = fallbackMediaDriver;
    }


    @Override
    @Transactional
    @CircuitBreaker(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    @Bulkhead(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    public void addMediaContent(List<MultipartFile> mediaContent, Long productId, Integer shopId)
            throws ProductNotFoundException, ShopDoesNotOwnProductException, UnsupportedContentType {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        List<PlainFile> plainFiles = mediaContent.stream().map(
                file -> {
                    try {
                        return new PlainFile(
                                file.getBytes(),
                                ContentType.parse(Objects.requireNonNull(file.getContentType()))
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).toList();

        List<String> names = mediaServiceClient.sendFiles(plainFiles);

        product.addMedia(names);

        productRepository.save(product);
    }

    @SuppressWarnings("unused")
    public void fallbackAddingMediaContent(List<MultipartFile> mediaContent, Long productId, Integer shopId)
            throws ProductNotFoundException, ShopDoesNotOwnProductException, UnsupportedContentType, IOException {

        log.debug("Fallback method for adding multiple media content was invoked");

        Product product = productRepository.findById(productId).orElseThrow(
                ProductNotFoundException::new
        );

        if (product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        List<FallbackMediaContent> mediaContents = new ArrayList<>();

        for(MultipartFile file: mediaContent) {

            FallbackMediaContent build = FallbackMediaContent.builder()
                    .contentType(
                            ContentType.parse(
                                    Objects.requireNonNull(file.getContentType())
                            )
                    )
                    .build();

            build.setProduct(product);

            mediaContents.add(build);
        }

        fallbackMediaDriver.saveFiles(
                mediaContent.stream()
                        .map(file -> {
                            try {
                                return file.getBytes();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }).toArray(byte[][]::new)
        );

        fallbackMediaContentRepository.saveAll(mediaContents);
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
                        image.getBytes(),
                        ContentType.parse(Objects.requireNonNull(image.getContentType()))
                )
        );

        product.setAvatar(imageName);

        productRepository.save(product);
    }

    @SuppressWarnings("unused")
    public void fallbackAddingAvatar(MultipartFile image, Long productId, Integer shopId)
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
                        ContentType.parseImage(
                                Objects.requireNonNull(image.getContentType())
                        )
                )
                .build();

        content.setProduct(product);

        fallbackMediaDriver.saveFile(image.getBytes());

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
    public List<MultipartFile> getMediaContent(Long productId)
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