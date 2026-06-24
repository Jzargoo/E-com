package com.jzargo.productservice.service;

import com.jzargo.productservice.entity.ContentType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface FallbackMediaDriver {

    String saveFile(byte[] content) throws IOException;

    List<String> saveFiles(byte[][] content) throws IOException;

    List<byte[]> getContent(List<String> mediaIds) throws IOException;

    byte[] getFile  (String mediaId) throws IOException;
}
