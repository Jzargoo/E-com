package com.jzargo.productservice.service;

import com.jzargo.productservice.exception.ProductNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface MediaService {

    void addMediaContent(List<MultipartFile> contents, Long productId, Integer shopId)
            throws IOException, ProductNotFoundException;

    void addAvatar(MultipartFile image, Long productId, Integer shopId)
            throws IOException, ProductNotFoundException;

    List<MultipartFile> getMediaContent(Long productId)
            throws IOException, ProductNotFoundException;

    MultipartFile getAvatar(Long productId)
                throws IOException, ProductNotFoundException;

}
