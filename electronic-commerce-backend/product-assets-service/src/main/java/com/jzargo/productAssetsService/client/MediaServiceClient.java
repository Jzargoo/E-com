package com.jzargo.productAssetsService.client;

import com.jzargo.productAssetsService.exception.CannotAddMediaFileException;
import com.jzargo.productAssetsService.model.PlainFile;

public interface MediaServiceClient {

    String sendFile(PlainFile file) throws CannotAddMediaFileException;

    PlainFile receiveFile(String mediaUri);

    String changeFile(PlainFile plainFile, Integer version, String prevUri);
}
