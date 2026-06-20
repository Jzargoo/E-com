package com.jzargo.productservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaServiceClient {
    void sendFiles(List<MultipartFile> files, Long productId);
}
