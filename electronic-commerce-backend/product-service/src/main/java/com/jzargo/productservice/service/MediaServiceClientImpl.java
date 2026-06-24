package com.jzargo.productservice.service;

import com.jzargo.productservice.model.PlainFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class MediaServiceClientImpl implements MediaServiceClient{

    @Override
    public List<String> sendFiles(List<PlainFile> files) {
        return List.of();
    }

    @Override
    public String sendFile(PlainFile file) {
        return "";
    }

    @Override
    public List<MultipartFile> receiveFiles(List<String> mediaIds) {
        return List.of();
    }

    @Override
    public MultipartFile receiveFile(String mediaIds) {
        return null;
    }
}
