package com.jzargo.media.service;

import com.jzargo.media.exceptions.CannotProcessException;
import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.storages.persistent.StorageType;
import com.jzargo.protobuf.MediaFile;

import java.io.IOException;
import java.util.List;

public interface MediaStorageService {
    String storeChunkFile(String key,String uploadId, byte[] byteArray);

    String initiateFile(MediaFile mediaFile, String key) throws IOException, WrongContentTypeException;


    void finishFileUploading(String key, String uploadId, List<String> tags) throws CannotProcessException;

    void abortMultipartFile(String key, String uploadId);
}
