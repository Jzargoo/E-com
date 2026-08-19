package com.jzargo.productAssetsService.driver;


import com.jzargo.productAssetsService.config.ApplicationPropertyStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Slf4j
@Component
public class FallbackMediaDriverNative implements FallbackMediaDriver{

    private final ApplicationPropertyStorage applicationPropertyStorage;

    public FallbackMediaDriverNative(ApplicationPropertyStorage applicationPropertyStorage) {
        this.applicationPropertyStorage = applicationPropertyStorage;
    }

    @Override
    public Mono<String> saveFile(Flux<DataBuffer> content, String key) throws IOException {
        String path = applicationPropertyStorage.getMedia().getPath();

        Files.createDirectories(Path.of(path));

        var image_name = UUID.randomUUID().toString();
        var pathToFile = Path.of(path + "/" + image_name);

        Files.createFile(pathToFile);

        Integer portionSize =
                applicationPropertyStorage.getFallbackMedia().getPortionSize();


        try (
                OutputStream stream = Files.newOutputStream(
                        pathToFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE
                )

        ){

            var remSize = 0;

            while  (remSize > 0) {

//                stream.write(
//                        content(portionSize)
//                );

                remSize -= Math.min(remSize, portionSize);
            }

        }


        return Mono.just(image_name);
    }

    @Override
    public Flux<DataBuffer> getFile(String name) {

        var path = Path.of(
                applicationPropertyStorage.getMedia().getPath() + "\\" + name
        );

        Integer portionSize =
                applicationPropertyStorage.getFallbackMedia().getPortionSize();

        return DataBufferUtils.readInputStream(
                () -> {
                    try(InputStream is = Files.newInputStream(path)) {
                        return is;
                    }
                },
                DefaultDataBufferFactory.sharedInstance,
                portionSize
        );

    }

    @Override
    public void deleteFile(String fileName) {
        var path = Path.of(
                applicationPropertyStorage.getMedia().getPath() + "\\" + fileName
        );

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
