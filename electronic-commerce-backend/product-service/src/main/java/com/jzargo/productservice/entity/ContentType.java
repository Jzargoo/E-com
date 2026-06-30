package com.jzargo.productservice.entity;

import com.jzargo.productservice.exception.UnsupportedContentType;

public enum ContentType {
    JPEG, PNG, MP4, UNKNOWN;

    public static ContentType parse(String mime)
            throws UnsupportedContentType {
        return switch (mime) {
            case "image/jpeg" -> JPEG;
            case "image/png" -> PNG;
            case "video/mp4" -> MP4;
            default -> throw new UnsupportedContentType();
        };
    }

    public static ContentType parseImage(String mime)
            throws UnsupportedContentType {
        return switch (mime) {
            case "image/jpeg" -> JPEG;
            case "image/png" -> PNG;
            default -> throw new UnsupportedContentType();
        };
    }
}
