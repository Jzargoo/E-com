package com.jzargo.productservice.service;

import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.model.PlainFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface MediaService {

    void addMediaContent(MultipartFile content, Long productId, Integer shopId)
            throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException;

    void addAvatar(MultipartFile image, Long productId, Integer shopId)
            throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException;

    List<PlainFile> getMediaContent(Long productId)
            throws IOException, ProductNotFoundException;

    PlainFile getAvatar(Long productId)
                throws IOException, ProductNotFoundException;

}
