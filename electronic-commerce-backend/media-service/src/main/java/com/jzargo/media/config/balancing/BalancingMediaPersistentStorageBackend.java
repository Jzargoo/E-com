package com.jzargo.media.config.balancing;

import com.jzargo.media.exceptions.BackendOutOfSpaceException;
import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.ErrorDuringAddingContent;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.media.storages.persistent.MediaPersistentStorageBackend;
import com.jzargo.media.storages.persistent.StorageType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BalancingMediaPersistentStorageBackend implements MediaPersistentStorageBackend {

    private final MediaPersistentStorageBackendRegistry registry;

    public BalancingMediaPersistentStorageBackend(MediaPersistentStorageBackendRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String storeFile(DownloadedFile file) throws CannotProcessException {

        while(!registry.isEmpty()){

            MediaPersistentStorageBackend next = registry
                    .next();

            try {

                if (next != null) {
                    return next.storeFile(file);
                }

            } catch (ErrorDuringAddingContent | BackendOutOfSpaceException e) {
                registry.removeBackend(next);

                //TODO: implement a watcher ....
            } catch (Exception e) {

                log.error("An exception was caught from a backend! ", e);

            }

        }

        throw new CannotProcessException();
    }

    @Override
    public String replaceFile(DownloadedFile file, String previousFileUri, String prevVersion) throws CannotProcessException {
        while(!registry.isEmpty()){

            MediaPersistentStorageBackend next = registry
                    .next();

            try {

                if (next != null) {
                    return next.replaceFile(file, previousFileUri, prevVersion);
                }

            } catch (ErrorDuringAddingContent | BackendOutOfSpaceException e) {
                registry.removeBackend(next);

                //TODO: implement a watcher ....
            }

        }

        throw new CannotProcessException();
    }

    @Override
    public void deleteFile(String fileUri) throws CannotProcessException {

        while(!registry.isEmpty()){

            MediaPersistentStorageBackend next = registry
                    .next();

            try {

                if (next != null) {
                    next.deleteFile(fileUri);

                    return;
                }

            } catch (ErrorDuringAddingContent | BackendOutOfSpaceException e) {
                registry.removeBackend(next);

                //TODO: implement a watcher ....
            }

        }

        throw new CannotProcessException();

    }

    @Override
    public StorageType getStorageType() {
        return null;
    }

    @Override
    public boolean existsByVersionedURI(String fileUri, String versionId) throws CannotProcessException {
        while(!registry.isEmpty()){

            MediaPersistentStorageBackend next = registry
                    .next();

            try {

                if (next != null) {
                    return next.existsByVersionedURI(fileUri, versionId);
                }

            } catch (ErrorDuringAddingContent | BackendOutOfSpaceException e) {
                registry.removeBackend(next);

                //TODO: implement a watcher ....
            }

        }

        throw new CannotProcessException();
    }

    @Override
    public boolean existsByURI(String fileUri) throws CannotProcessException {

        while(!registry.isEmpty()){

            MediaPersistentStorageBackend next = registry
                    .next();

            try {

                if (next != null) {
                    return next.existsByURI(fileUri);
                }

            } catch (ErrorDuringAddingContent | BackendOutOfSpaceException e) {
                registry.removeBackend(next);

                //TODO: implement a watcher ....
            }

        }

        throw new CannotProcessException();
    }

    @Override
    public DownloadedFile getFile(String fileUri) throws CannotProcessException, WrongContentTypeException {

        while(!registry.isEmpty()){

            MediaPersistentStorageBackend next = registry
                    .next();

            try {

                if (next != null) {
                    return next.getFile(fileUri);
                }

            } catch (ErrorDuringAddingContent | BackendOutOfSpaceException e) {
                registry.removeBackend(next);

                //TODO: implement a watcher ....
            }

        }

        throw new CannotProcessException();

    }

    @Override
    public void register() {} // this method will not be used for this implementation
}
