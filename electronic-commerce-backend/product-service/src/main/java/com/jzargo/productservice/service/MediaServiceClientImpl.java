package com.jzargo.productservice.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class MediaServiceClientImpl implements MediaServiceClient{
    @Override
    public void sendFiles(List<MultipartFile> files, Long productId) {

    }
}
