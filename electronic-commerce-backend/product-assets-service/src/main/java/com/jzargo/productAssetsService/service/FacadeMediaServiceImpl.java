package com.jzargo.productAssetsService.service;

import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.model.PlainFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
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
        log.debug("Adding media content to product {} ", productId);
        mediaService.addMediaContent(content, productId, shopId);
        log.trace("Added media content to product {} ", productId);
    }

    @Override
    @Async("media-service-executor")
    public void addAvatar(MultipartFile image, Long productId, Integer shopId) throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException {
        log.debug("Adding avatar to product {} ", productId);
        mediaService.addAvatar(image, productId, shopId);
        log.trace("Added avatar to product {} ", productId);
    }

    @Override
    public List<PlainFile> getMediaContent(Long productId) throws IOException, ProductNotFoundException {
        log.debug("Getting media content from product {} ", productId);
        return mediaService.getMediaContent(productId);
    }

    @Override
    public PlainFile getAvatar(Long productId) throws IOException, ProductNotFoundException {
        log.debug("Getting avatar from product {} ", productId);
        return mediaService.getAvatar(productId);
    }

}
