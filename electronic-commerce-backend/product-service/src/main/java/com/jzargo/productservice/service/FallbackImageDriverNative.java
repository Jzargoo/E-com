package com.jzargo.productservice.service;

import com.jzargo.productservice.config.ApplicationPropertyStorage;
import lombok.SneakyThrows;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FallbackImageDriverNative implements FallbackImageDriver{

    private final ApplicationPropertyStorage applicationPropertyStorage;

    public FallbackImageDriverNative (ApplicationPropertyStorage applicationPropertyStorage) {
        this.applicationPropertyStorage = applicationPropertyStorage;
    }

    @Override
    public String saveFile(byte[] image) throws IOException  {
        String path = applicationPropertyStorage.getImage().getPath();

        Files.createDirectories(Path.of(path));

        var image_name = UUID.randomUUID() + ".png";
        var pathToFile = Path.of(path + "/" + image_name);

        Files.createFile(pathToFile);

        Files.write(pathToFile, image);

        return image_name;
    }

    @Override
    public List<String> saveFiles(byte[][] images) throws IOException {
        return List.of();
    }

    @Override
    public List<MultipartFile> getFiles(List<String> names) throws IOException {
        return List.of();
    }

    @Override
    public byte[] getFile(String name) throws IOException {
        return new byte[0];
    }

}
