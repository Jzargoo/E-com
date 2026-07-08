package com.jzargo.media.service;

import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.protobuf.MediaFile;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class MediaStorageServiceImpl implements MediaStorageService {


    @Override
    public void storeChunkFile(byte[] byteArray) {

    }

    @Override
    public String initiateFile(MediaFile mediaFile) throws IOException, WrongContentTypeException {
        MediaHelper.checkContentType(mediaFile);


        return "";
    }
}
