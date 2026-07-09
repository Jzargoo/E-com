package com.jzargo.media.service;

import com.jzargo.media.config.ApplicationPropertyStorage;
import com.jzargo.media.exceptions.CannotProcessException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SmartBuffersServiceStreamImpl implements SmartBuffersService {
    // PROPERTIES
    private final Integer threshold;
    private final Integer bufferSize;

    private final Map<String, ByteArrayOutputStream> buffers;

    private final MediaStorageService mediaStorageService;

    public SmartBuffersServiceStreamImpl(
            ApplicationPropertyStorage applicationPropertyStorage,
            MediaStorageService mediaStorageService) {

        this.buffers = new ConcurrentHashMap<>();

        this.threshold = applicationPropertyStorage.getAws()
                .getSmartStreamProperties()
                .getThreshold();

        this.bufferSize = applicationPropertyStorage.getAws()
                .getSmartStreamProperties()
                .getBufferSize();

        this.mediaStorageService = mediaStorageService;
    }

    @Override
    public void addBuffer(String key) {
        buffers
                .put(
                        key,
                        new ByteArrayOutputStream(bufferSize)
                );
    }

    @Override
    public String addIntoBuffer(String key, String uploadId, byte[] contentChunk) throws CannotProcessException {
        ByteArrayOutputStream outputStream = buffers.get(key);

        try {
            outputStream.write(contentChunk);
        } catch (IOException e) {
            throw new CannotProcessException();
        }

        if (outputStream.size() >= threshold) {
            return mediaStorageService.storeChunkFile(key, uploadId, outputStream.toByteArray());
        }

        return null;
    }

    @Override
    public String finishBuffer(String key, String uploadId) throws CannotProcessException{
        ByteArrayOutputStream buffer = buffers.get(key);


        try {
            buffer.close();
        } catch (IOException ignored) {}

        buffers.remove(key);

        return mediaStorageService.storeChunkFile(key, uploadId, buffer.toByteArray());
    }
}