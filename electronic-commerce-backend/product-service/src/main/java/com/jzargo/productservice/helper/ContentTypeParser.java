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

    public static String getMediaPostfix(ContentType contentType) throws UnsupportedContentType {
        return switch (contentType) {
            case JPEG -> ".jpg";
            case PNG -> ".png";
            case WEBP -> ".webp";
            case MP4 -> ".mp4";
            case WEBM -> ".webm";
            default -> throw new UnsupportedContentType();
        };
    }

    public static String parseIntoMime(ContentType contentType) {
        return switch (contentType) {
            case JPEG -> "application/jpeg";
            case PNG -> "application/png";
            case WEBP -> "application/webp";
            case MP4 -> "application/mp4";
            case WEBM -> "application/webm";
            default -> throw new UnsupportedContentType();
        };
    }
}
