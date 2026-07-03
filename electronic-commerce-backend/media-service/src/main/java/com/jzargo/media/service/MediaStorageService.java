package com.jzargo.media.service;

import com.jzargo.media.exceptions.WrongContentTypeException;
import com.jzargo.protobuf.PlainFile;

import java.io.IOException;
import java.util.List;

public interface MediaStorageService {
    void storeFiles(List<PlainFile> files) throws WrongContentTypeException, IOException;
}
