package com.jzargo.media.storages.persistent;

import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.config.balancing.MediaPersistentStorageBackendRegistry;
import com.jzargo.media.exceptions.*;
import com.jzargo.media.exceptions.FileAlreadyExistsException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.media.model.DownloadedFile;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class MediaPersistentStorageBackendNative implements MediaPersistentStorageBackend {

    private final ApplicationPropertyStorage applicationPropertyStorage;
    private final MediaPersistentStorageBackendRegistry mediaPersistentStorageBackendRegistry;

    public MediaPersistentStorageBackendNative (
            ApplicationPropertyStorage applicationPropertyStorage,
            MediaPersistentStorageBackendRegistry mediaPersistentStorageBackendRegistry
    ) {
        this.applicationPropertyStorage = applicationPropertyStorage;
        this.mediaPersistentStorageBackendRegistry = mediaPersistentStorageBackendRegistry;
    }

    @Override
    public String storeFile(DownloadedFile file)
            throws ErrorDuringAddingContent,
            BackendOutOfSpaceException,
            CannotProcessException {

        Path lockPath = getLockPath(file.getFileUri());

        try (
                FileChannel fl = FileChannel.open(
                        lockPath,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE
                );

                var ignored = fl.lock()
        ){

            Path fileUri = getFileUri(file.getFileUri());

            Path versionUri = getVersionUri(file.getFileUri());

            if (Files.exists(fileUri) || Files.exists(versionUri)) {
                throw new CannotProcessException();
            }



            Files.copy(
                    file.getContent(), fileUri,
                    StandardCopyOption.REPLACE_EXISTING
            );

            writeVersion(versionUri, file.getVersionId());

            return file.getFileUri();

        } catch (IOException ex) {

            deleteFile(file.getFileUri());

            throw new ErrorDuringAddingContent(ex.getMessage());
        }

    }

    private void writeVersion(Path versionUri, String versionId) throws IOException {
        Files.writeString(versionUri, versionId);
    }

    private String readVersion(Path versionUri) throws IOException {
        byte[] bytes = Files.readAllBytes(versionUri);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private Path getLockPath(String fileUri) {

        var options= applicationPropertyStorage.getNativeStorageOptions();

        var path = Path.of(
               "%s%s%s%s%s%s"
                       .formatted(
                               options.getSavingPath(),
                               FileSystems.getDefault().getSeparator(),
                               options.getLockPath(),
                               FileSystems.getDefault().getSeparator(),
                               fileUri,
                               options.getLockSuffix()
                       )
        );

        try {

            Files.createDirectories(
                    path.getParent()
            );

        } catch (IOException ignored) {}

        return path;

    }

    private Path getVersionUri(String fileUri) {
        var options = applicationPropertyStorage.getNativeStorageOptions();

        var path = Path.of(
                "%s%s%s%s%s%s"
                        .formatted(
                                options.getSavingPath(),
                                FileSystems.getDefault().getSeparator(),
                                options.getVersionPath(),
                                FileSystems.getDefault().getSeparator(),
                                fileUri,
                                options.getVersionSuffix()
                        )
        );

        try{
            Files.createDirectories(path.getParent());
        } catch (IOException ignored) {}

        return path;
    }

    private Path getFileUri(String fileUri) {
        String savingPath = applicationPropertyStorage.getNativeStorageOptions().getSavingPath();

        var path =  Path.of(
                "%s%s%s"
                        .formatted(
                                savingPath, FileSystems.getDefault().getSeparator(), fileUri
                        )
        );

        try {
            Files.createDirectories(path.getParent());
        } catch (IOException ignored) {}

        return path;
    }

    @Override
    public String replaceFile(DownloadedFile file, String previousFileUri, String prevVersion) throws CannotProcessException {

        Path lockPath = getLockPath(file.getFileUri());

        try (
                FileChannel fl = FileChannel.open(
                        lockPath,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE
                );

                var ignored = fl.lock()
        ) {

            Path previousFilePath = getFileUri(previousFileUri);
            Path previousVersionPath = getVersionUri(previousFileUri);

            if (!Files.exists(previousFilePath)) {
                throw new CannotProcessException();
            }

            String actualVersion = readVersion(previousVersionPath);

            if (!actualVersion.equals(prevVersion)) {
                throw new CannotProcessException();
            }

            Path newFilePath = getFileUri(file.getFileUri());
            Path newVersionPath = getVersionUri(file.getFileUri());

            Files.copy(
                    file.getContent(),
                    newFilePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            writeVersion(
                    newVersionPath,
                    file.getVersionId()
            );

            if (!previousFileUri.equals(file.getFileUri())) {

                Files.deleteIfExists(previousFilePath);

                Files.deleteIfExists(previousVersionPath);

            }

            return file.getFileUri();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public void deleteFile(String fileUri) throws CannotProcessException {

        try {

            Files.deleteIfExists(
                    getFileUri(fileUri)
            );

            Files.deleteIfExists(
                    getVersionUri(fileUri)
            );

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public StorageType getStorageType() {
        return StorageType.NATIVE_DISK;
    }

    @Override
    public boolean existsByVersionedURI(String fileUri, String versionId) throws CannotProcessException {
        Path versionUri = getVersionUri(fileUri);

        try {

            return existsByURI(fileUri) && readVersion(versionUri).equals(versionId);


        } catch (IOException e) {

            throw new CannotProcessException();

        }
    }

    @Override
    public DownloadedFile getFile(String fileUri) throws CannotProcessException, WrongContentTypeException {

        if(!existsByURI(fileUri)){
            throw new CannotProcessException();
        }

        try {

            String versionId = readVersion(
                    getVersionUri(fileUri)
            );

            String[] split = fileUri.split("\\.");

            return DownloadedFile.builder()
                    .content(
                            Files.newInputStream(
                                    getFileUri(fileUri)
                            )
                    )
                    .contentLength(
                            Files.size(
                                    getFileUri(fileUri)
                            )
                    )
                    .versionId(versionId)
                    .contentType(
                            MediaHelper.getTypeByPostfix(
                                    split[split.length - 1]
                            )
                    )
                    .fileUri(fileUri)
                    .build();

        } catch (IOException e) {
           throw new CannotProcessException();
        }

    }

    @Override
    @PostConstruct
    public void register() {
        mediaPersistentStorageBackendRegistry.addBackend(this);
    }

    @Override
    public boolean existsByURI(String uri) throws CannotProcessException {
        return Files.exists(
                getFileUri(uri)
        );
    }
}