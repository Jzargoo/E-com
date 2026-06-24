package com.jzargo.productservice.service;

import com.jzargo.productservice.config.ApplicationPropertyStorage;
import com.jzargo.productservice.entity.ContentType;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class FallbackMediaDriverNative implements FallbackMediaDriver{

    private final ApplicationPropertyStorage applicationPropertyStorage;

    public FallbackMediaDriverNative(ApplicationPropertyStorage applicationPropertyStorage) {
        this.applicationPropertyStorage = applicationPropertyStorage;
    }

    @Override
    public String saveFile(byte[] content) throws IOException  {
        String path = applicationPropertyStorage.getMedia().getPath();

        Files.createDirectories(Path.of(path));

        var image_name = UUID.randomUUID().toString();
        var pathToFile = Path.of(path + "/" + image_name);

        Files.createFile(pathToFile);

        Files.write(pathToFile, content);

        return image_name;
    }

    @Override
    public List<String> saveFiles(byte[][] content) throws IOException {

        List<String> output = new ArrayList<>();

        for (byte[] file: content) {
            output.add(
                saveFile(file)
            );
        }

        return output;
    }

    @Override
    public List<byte[]> getContent(List<String> mediaIds) throws IOException {

        List<byte[]> files = new ArrayList<>();

        for (String mediaId: mediaIds) {
            files.add(
                    getFile(mediaId)
            );
        }

        return files;
    }

    @Override
    public byte[] getFile(String name) throws IOException {
        try {

            var path = java.nio.file.Path.of(
                        new URI(
                                applicationPropertyStorage.getMedia().getPath() + "/" + name
                        )
            );

            return Files.readAllBytes(path);


        } catch (URISyntaxException e) {
            log.error("The incorrect uri syntax exception occurred with media id {}", name);
            throw new IOException("URI problems");
        }
    }
}
