package com.jzargo.productservice.client;

import com.jzargo.productservice.model.PlainFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaServiceClient {
    List<String> sendFiles(List<PlainFile> files);

    String sendFile(PlainFile file);

    List<MultipartFile> receiveFiles(List<String> mediaIds);

    MultipartFile receiveFile(String mediaId);

}
