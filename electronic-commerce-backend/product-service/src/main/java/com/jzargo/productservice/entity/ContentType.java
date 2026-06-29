package com.jzargo.productservice.entity;

public enum ContentType {
    JPEG, PNG, MP4, UNKNOWN;

    public static ContentType parse(String mime){
        return switch (mime) {
            case "image/jpeg" -> JPEG;
            case "image/png" -> PNG;
            case "video/mp4" -> MP4;
            default -> UNKNOWN;
        };
    }
}
