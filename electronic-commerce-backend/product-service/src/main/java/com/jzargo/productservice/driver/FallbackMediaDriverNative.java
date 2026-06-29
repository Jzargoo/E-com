package com.jzargo.productservice.driver;

import com.jzargo.productservice.config.ApplicationPropertyStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        var path = Path.of(
                applicationPropertyStorage.getMedia().getPath() + "\\" + name
        );

        return Files.readAllBytes(path);
    }
}
