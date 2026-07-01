package com.jzargo.media.exceptions;

public class CorruptedMediaRequest extends RuntimeException {
    public CorruptedMediaRequest(String message) {
        super(message);
    }
}
