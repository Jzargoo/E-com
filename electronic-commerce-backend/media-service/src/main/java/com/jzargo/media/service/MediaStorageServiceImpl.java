package com.jzargo.media.service;

import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.media.helper.MediaHelper;
import com.jzargo.protobuf.PlainFile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class MediaStorageServiceImpl implements MediaStorageService {

    @Override
    public void storeFiles(List<PlainFile> files) throws WrongContentTypeException, IOException {
        for  (PlainFile file : files) {
            MediaHelper.checkContentType(file);
        }


    }
}
