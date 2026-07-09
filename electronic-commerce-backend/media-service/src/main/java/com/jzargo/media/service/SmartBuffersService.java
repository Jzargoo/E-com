package com.jzargo.media.service;

import com.jzargo.media.exceptions.CannotProcessException;


public interface SmartBuffersService {
    void addBuffer(String key);
    String finishBuffer(String key, String uploadId) throws CannotProcessException;
    String addIntoBuffer(String key,String uploadId, byte[] contentChunk) throws CannotProcessException;
}
