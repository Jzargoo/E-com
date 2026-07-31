package com.jzargo.productservice.service;

import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.model.PlainFile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Qualifier("asyncMediaService")
public class FacadeMediaServiceImpl implements MediaService {

    private final MediaService mediaService;

    public FacadeMediaServiceImpl(@Qualifier("internalMediaService") MediaService internalMediaService) {
        this.mediaService = internalMediaService;
    }

    @Override
    @Async("media-service-executor")
    public void addMediaContent(MultipartFile content, Long productId, Integer shopId) throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException {
        mediaService.addMediaContent(content, productId, shopId);
    }

    @Override
    @Async("media-service-executor")
    public void addAvatar(MultipartFile image, Long productId, Integer shopId) throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException {
        mediaService.addAvatar(image, productId, shopId);
    }

    @Override
    public List<PlainFile> getMediaContent(Long productId) throws IOException, ProductNotFoundException {
        return mediaService.getMediaContent(productId);
    }

    @Override
    public PlainFile getAvatar(Long productId) throws IOException, ProductNotFoundException {
        return mediaService.getAvatar(productId);
    }

}
