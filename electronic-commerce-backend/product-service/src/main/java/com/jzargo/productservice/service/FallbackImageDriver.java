package com.jzargo.productservice.service;

import com.jzargo.productservice.entity.ContentType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface FallbackImageDriver {

    String saveFileIntoStorage(MultipartFile content) throws IOException;

    void saveMetadataInDatabase(ContentType contentType, String mediaId, Long productId);

    default void save(MultipartFile file, Long productId) throws IOException{
        String mediaId = saveFileIntoStorage(file);
        saveMetadataInDatabase();
    }

    List<String> saveFiles(List<MultipartFile> content) throws IOException;

    List<MultipartFile> getContent(List<String> mediaIds) throws IOException;

    byte[] getFile(String mediaId) throws IOException;
}
