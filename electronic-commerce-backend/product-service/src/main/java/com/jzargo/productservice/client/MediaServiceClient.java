package com.jzargo.productservice.client;

import com.jzargo.productservice.exception.CannotAddMediaFileException;
import com.jzargo.productservice.model.PlainFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaServiceClient {

    String sendFile(PlainFile file) throws CannotAddMediaFileException;

    List<PlainFile> receiveFiles(List<String> mediaIds);

    MultipartFile receiveFile(String mediaId);

}
