package com.jzargo.productservice.service;


import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.repository.ProductRepository;
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
    private final ImageDriver imageDriver;
    private final ProductRepository productRepository;

    public MediaServiceImpl(ImageDriver imageDriver, ProductRepository productRepository) {
        this.imageDriver = imageDriver;
        this.productRepository = productRepository;
    }


    @Override
    @Transactional
    public void addMediaContent(List<MultipartFile> mediaContent, Long productId, Integer shopId)
    throws ProductNotFoundException{

        Product product = productRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        try {
            List<String> names = imageDriver.saveFiles(images);

            product.addImages(names);
        } catch (IOException e) {
            log.error("Cannot save all images. Unexpected error occurred", e);
        }

        productRepository.save(product);
    }

    @Override
    @Transactional
    public void addAvatar(byte[] image, Long productId, Integer shopId)
            throws IOException, ProductNotFoundException {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        String imageName = imageDriver.saveFile(image);

        product.setAvatar(imageName);

        productRepository.save(product);
    }

    @Override
    public byte[] getAvatar(Long productId)
        throws ProductNotFoundException, IOException {

        String avatar = productRepository
                .findById(productId)
                .map(Product::getAvatar)
                .orElseThrow(ProductNotFoundException::new);

        return imageDriver.getImage(avatar);
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