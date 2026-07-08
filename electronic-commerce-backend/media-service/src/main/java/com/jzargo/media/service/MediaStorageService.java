package com.jzargo.media.service;

import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.protobuf.MediaFile;

import java.io.IOException;

public interface MediaStorageService {
    void storeChunkFile(byte[] byteArray);

    String initiateFile(MediaFile mediaFile) throws IOException, WrongContentTypeException;
}
