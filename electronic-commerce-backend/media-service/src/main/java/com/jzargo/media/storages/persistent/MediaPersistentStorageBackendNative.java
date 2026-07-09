package com.jzargo.media.storages.persistent;

import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.ErrorDuringAddingContent;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.media.model.DownloadedFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

public class MediaPersistentStorageBackendNative implements MediaPersistentStorageBackend {
    private final ApplicationPropertyStorage applicationPropertyStorage;

    public MediaPersistentStorageBackendNative(ApplicationPropertyStorage applicationPropertyStorage) {
        this.applicationPropertyStorage = applicationPropertyStorage;
    }

    @Override
    public String storeFile(DownloadedFile file) throws ErrorDuringAddingContent, CannotProcessException {
        String savingPath = applicationPropertyStorage.getNativeStorageOptions().getSavingPath();


        try {
            Files.createDirectories(
                    Path.of(savingPath)
            );

            String separator = FileSystems.getDefault().getSeparator();

            Path path = Path.of(
                    "%s%s%s".formatted(
                            savingPath,
                            separator,
                            file.getFileUri()
                    )
            );

            Files.copy(file.getContent(), path, StandardCopyOption.REPLACE_EXISTING);

            return file.getFileUri();

        } catch (IOException e) {
            deleteFile(file.getFileUri());
            throw new ErrorDuringAddingContent("Cannot create directories to path { %s }".formatted(savingPath));
        }
    }

    @Override
    public String replaceFile(DownloadedFile file, String otherUri) throws CannotProcessException {
        file.setFileUri(otherUri);

        return storeFile(file);
    }

    @Override
    public void deleteFile(String fileUri) throws CannotProcessException {

        var path = getPathByFileUri(fileUri);

        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new CannotProcessException();
        }

    }

    @Override
    public StorageType getStorageType() {
        return StorageType.NATIVE_DISK;
    }

    @Override
    public boolean existsByURI(String fileUri) throws CannotProcessException {

        Path pathByFileUri = getPathByFileUri(fileUri);

        try {

            try(var stream = Files.newDirectoryStream(pathByFileUri.getParent())){

                for(var file : stream){

                    if (
                            file.toString().startsWith(fileUri)
                    ) {
                        return true;
                    }

                }

            };

        } catch (IOException e) {
            throw new CannotProcessException();
        }

        return false;
    }

    private Path getPathByFileUri(String fileUri){
        String separator = FileSystems.getDefault().getSeparator();

        String savingPath =  applicationPropertyStorage.getNativeStorageOptions().getSavingPath();

        return Path.of(
                "%s%s%s".formatted(
                        savingPath,
                        separator,
                        fileUri
                )
        );
    }

    @Override
    public DownloadedFile getFile(String fileUri) throws CannotProcessException, WrongContentTypeException {
        try {
            InputStream inputStream = Files.newInputStream(
                    getPathByFileUri(fileUri)
            );

            String[] split = fileUri.split("\\.");

            var content = MediaHelper.getTypeByPostfix(split[split.length - 1]);

            return MediaHelper.createFileRepresentation(
                    inputStream,
                    MediaHelper.parseToMime(content),
                    fileUri
            );

        } catch (IOException e) {
            throw new CannotProcessException();
        }
    }
}