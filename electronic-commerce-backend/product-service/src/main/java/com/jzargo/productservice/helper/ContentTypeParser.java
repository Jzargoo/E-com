package com.jzargo.productservice.helper;

import com.jzargo.productservice.exception.UnsupportedContentType;
import com.jzargo.protobuf.ContentType;

public class ContentTypeParser {
    public static ContentType parse(String mime)
            throws UnsupportedContentType {
        return switch (mime) {
            case "image/jpeg" -> ContentType.JPEG;
            case "image/png" -> ContentType.PNG;
            case "image/webp" -> ContentType.WEBP;
            case "video/mp4" -> ContentType.MP4;
            case "video/webm" -> ContentType.WEBM;
            default -> throw new UnsupportedContentType();
        };
    }

    public static ContentType parseImage(String mime)
            throws UnsupportedContentType {
        return switch (mime) {
            case "image/jpeg" -> ContentType.JPEG;
            case "image/png" -> ContentType.PNG;
            case "image/webp" -> ContentType.WEBP;
            default -> throw new UnsupportedContentType();
        };
    }
}
