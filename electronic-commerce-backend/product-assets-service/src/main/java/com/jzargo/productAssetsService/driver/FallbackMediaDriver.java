package com.jzargo.productAssetsService.driver;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

public interface FallbackMediaDriver {
    Mono<String> saveFile(Flux<DataBuffer> content, String key) throws IOException;

    Flux<DataBuffer> getFile(String mediaId);

    default void deleteFiles(List<String> fileNames) throws IOException{
        for (String fileName: fileNames) {
            deleteFile(fileName);
        }
    }

    void deleteFile(String fileName);

}
