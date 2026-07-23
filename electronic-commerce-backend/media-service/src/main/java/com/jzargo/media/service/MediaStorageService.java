package com.jzargo.media.service;

import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.FileNotFoundException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.model.DownloadedFile;
import com.jzargo.media.storages.persistent.StorageType;
import com.jzargo.protobuf.MediaFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface MediaStorageService {
    String storeChunkFile(String key,String uploadId, byte[] byteArray);

    String initiateFile(MediaFile mediaFile, String key) throws IOException, WrongContentTypeException;


    void finishFileUploading(String key, String uploadId, List<String> tags, boolean isVideo) throws CannotProcessException;

    void abortMultipartFile(String key, String uploadId);

    DownloadedFile getFileStream(String uri) throws CannotProcessException, FileNotFoundException, WrongContentTypeException;
}
