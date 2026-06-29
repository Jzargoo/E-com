package com.jzargo.productservice.driver;

import java.io.IOException;
import java.util.List;

public interface FallbackMediaDriver {

    String saveFile(byte[] content) throws IOException;

    List<String> saveFiles(byte[][] content) throws IOException;

    List<byte[]> getContent(List<String> mediaIds) throws IOException;

    byte[] getFile  (String mediaId) throws IOException;
}
