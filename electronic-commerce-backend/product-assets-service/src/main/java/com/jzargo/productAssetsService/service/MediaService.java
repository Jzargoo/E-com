package com.jzargo.productAssetsService.service;

import com.jzargo.productAssetsService.exception.*;
import com.jzargo.productAssetsService.model.PlainFile;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public interface MediaService {

    void addMediaContent(MultipartFile content, Long productId, Integer shopId)
            throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException, CannotAddMediaFileException, UnsupportedContentType;

    void addAvatar(MultipartFile image, Long productId, Integer shopId)
            throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException, UnsupportedContentType;

    Flux<DataBuffer> getMediaContent(Long assetId)
            throws IOException, AssetNotFoundException;

    List<Long> findIdsByProductId(Long productId);

    PlainFile getAvatar(Long productId)
                throws IOException, ProductNotFoundException;

}
