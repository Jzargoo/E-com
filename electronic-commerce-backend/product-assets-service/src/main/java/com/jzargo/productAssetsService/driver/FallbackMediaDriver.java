package com.jzargo.productAssetsService.driver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public interface FallbackMediaDriver {
    default List<InputStream> getContent(List<String> mediaIds) throws IOException {
        List<InputStream> files = new ArrayList<>();

        for (String mediaId: mediaIds) {
            files.add(
                    getFile(mediaId)
            );
        }

        return files;

    }

    String saveFile(InputStream content) throws IOException;

    InputStream getFile  (String mediaId) throws IOException;

    default void deleteFiles(List<String> fileNames) throws IOException{
        for (String fileName: fileNames) {
            deleteFile(fileName);
        }
    }

    void deleteFile(String fileName) throws IOException;

}
