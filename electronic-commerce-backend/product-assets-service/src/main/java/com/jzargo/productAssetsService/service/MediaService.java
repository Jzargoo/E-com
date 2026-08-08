package com.jzargo.productAssetsService.service;

import com.jzargo.productAssetsService.exception.CannotAddMediaFileException;
import com.jzargo.productAssetsService.exception.ProductNotFoundException;
import com.jzargo.productAssetsService.exception.ShopDoesNotOwnProductException;
import com.jzargo.productAssetsService.exception.UnsupportedContentType;
import com.jzargo.productAssetsService.model.PlainFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public interface MediaService {

    void addMediaContent(MultipartFile content, Long productId, Integer shopId)
            throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException, CannotAddMediaFileException, UnsupportedContentType;

    void addAvatar(MultipartFile image, Long productId, Integer shopId)
            throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException, UnsupportedContentType;

    List<PlainFile> getMediaContent(Long productId)
            throws IOException, ProductNotFoundException;

    PlainFile getAvatar(Long productId)
                throws IOException, ProductNotFoundException;

}
