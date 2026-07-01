package com.jzargo.media.service;

import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.exceptions.ErrorDuringAddingContent;
import com.jzargo.protobuf.PlainFile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class MediaStorageServiceNative implements MediaStorageService{
    private final ApplicationPropertyStorage applicationPropertyStorage;

    public MediaStorageServiceNative(ApplicationPropertyStorage applicationPropertyStorage) {
        this.applicationPropertyStorage = applicationPropertyStorage;
    }

    @Override
    public List<String> storeFiles(List<PlainFile> files) {
        return List.of();
    }

    @Override
    public String storeFile(PlainFile file) {
        String savingPath = applicationPropertyStorage.getNativeStorageOptions().getSavingPath();

        String id = UUID.randomUUID().toString();

        try {
            Files.createDirectories(
                    Path.of(savingPath)
            );

            String separator = FileSystems.getDefault().getSeparator();

            Files.write(
                    Path.of(
                            "%s%s%s.%s".formatted(
                                    savingPath,
                                    separator,
                                    id,
                                    file.getContentType().toString().toLowerCase()
                            )
                    ),
                    file.getContent().toByteArray()
            );

            return id;
        } catch (IOException e) {
            deleteFile(id);
           throw new ErrorDuringAddingContent("Cannot create directories to path { %s }".formatted(savingPath));
        }
    }

    @Override
    public void replaceFile(PlainFile plainFile, String id) {

    }

    @Override
    public void deleteFile(String id) {

    }
}
