package com.jzargo.productAssetsService.driver;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    Mono<String> saveFile(Flux<DataBuffer> content, String key);

    InputStream getFile  (String mediaId) throws IOException;

    default void deleteFiles(List<String> fileNames) throws IOException{
        for (String fileName: fileNames) {
            deleteFile(fileName);
        }
    }

    void deleteFile(String fileName) throws IOException;

}
