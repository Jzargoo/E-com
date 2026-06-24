package com.jzargo.productservice.service;


import com.jzargo.productservice.entity.ContentType;
import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.model.PlainFile;
import com.jzargo.productservice.repository.ProductRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true) // if not provided, data must be in immutable state
public class MediaServiceImpl implements MediaService {
    private final MediaServiceClient mediaServiceClient;
    private final ProductRepository productRepository;

    public MediaServiceImpl(MediaServiceClient mediaServiceClient, ProductRepository productRepository) {
        this.mediaServiceClient = mediaServiceClient;
        this.productRepository = productRepository;
    }


    @Override
    @Transactional
    @CircuitBreaker(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    @Bulkhead(name = "mediaService", fallbackMethod = "fallbackAddingMediaContent")
    public void addMediaContent(List<MultipartFile> mediaContent, Long productId, Integer shopId)
        throws ProductNotFoundException {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        List<String> names = mediaServiceClient.sendFiles(mediaContent);

        product.addImages(names);

        productRepository.save(product);
    }

    public void fallbackAddingMediaContent(List<MultipartFile> mediaContent, Long productId, Integer shopId){
        log.debug("Fallback method for adding multiple media content was invoked");
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "mediaService", fallbackMethod = "fallbackAddingMediaFile")
    @Bulkhead(name = "mediaService", fallbackMethod = "fallbackAddingMediaFile")
    public void addAvatar(MultipartFile image, Long productId, Integer shopId)
            throws IOException, ProductNotFoundException {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        String imageName = mediaServiceClient.sendFile(image);

        product.setAvatar(imageName);

        productRepository.save(product);
    }

    public void fallbackAddingMediaFile(MultipartFile image, Long productId, Integer shopId){
        log.debug("Fallback method for adding one file was invoked");
    }

    @Override
    public PlainFile getAvatar(Long productId)
        throws ProductNotFoundException, IOException {

        String avatar = productRepository
                .findById(productId)
                .map(Product::getAvatar)
                .orElseThrow(ProductNotFoundException::new);

        MultipartFile multipartFile = mediaServiceClient.receiveFile(avatar);

        ContentType contentType = ContentType.valueOf(multipartFile.getContentType());

        return new PlainFile(multipartFile.getBytes(), contentType);

    }

    @Override
    public List<MultipartFile> getMediaContent(Long productId)
        throws ProductNotFoundException, IOException {

        List<String> allImages = productRepository
                .findById(productId)
                .map(Product::getImages)
                .orElseThrow(
                        ProductNotFoundException::new
                );

        return imageDriver.getImages(allImages);
    }
}