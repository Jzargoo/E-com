package com.jzargo.productservice.driver;

import com.jzargo.productservice.config.ApplicationPropertyStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    public String saveFile(InputStream content, Long length) throws IOException  {
        String path = applicationPropertyStorage.getMedia().getPath();

        Files.createDirectories(Path.of(path));

        var image_name = UUID.randomUUID().toString();
        var pathToFile = Path.of(path + "/" + image_name);

        Files.createFile(pathToFile);

        Integer portionSize =
                applicationPropertyStorage.getFallbackMedia().getPortionSize();

        long l = Math.ceilDiv(length, portionSize.longValue()) + 1;

        try (
                OutputStream  stream = Files.newOutputStream(
                        pathToFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE
                )

        ){

            for (long i = 0; i < l; i++) {

                stream.write(
                        content.readNBytes(portionSize)
                );

            }

        }


        return image_name;
    }

    @Override
    public InputStream getFile(String name) throws IOException {
        var path = Path.of(
                applicationPropertyStorage.getMedia().getPath() + "\\" + name
        );

        return Files.newInputStream(path);
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
        var path = Path.of(
                applicationPropertyStorage.getMedia().getPath() + "\\" + fileName
        );

        Files.deleteIfExists(path);
    }
}
