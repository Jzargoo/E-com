package com.jzargo.productAssetsService.helper;

import com.jzargo.productAssetsService.exception.UnsupportedContentType;
import com.jzargo.protobuf.ContentType;

public class ContentTypeParser {

    private ContentTypeParser() {}

    public static ContentType parse(String mime)
            throws UnsupportedContentType {
        return switch (mime) {
            case "image/jpeg" -> ContentType.JPEG;
            case "image/png" -> ContentType.PNG;
            case "image/webp" -> ContentType.WEBP;
            case "video/mp4" -> ContentType.MP4;
            case "video/webm" -> ContentType.WEBM;
            default -> throw new UnsupportedContentType("Unsupported mime type: " + mime);
        };
    }

    public static ContentType parseImage(String mime)
            throws UnsupportedContentType {
        return switch (mime) {
            case "image/jpeg" -> ContentType.JPEG;
            case "image/png" -> ContentType.PNG;
            case "image/webp" -> ContentType.WEBP;
            default -> throw new UnsupportedContentType("Cannot parse mime image to image content type: " + mime);
        };
    }

    public static String getMediaPostfix(ContentType contentType) throws UnsupportedContentType {
        return switch (contentType) {
            case JPEG -> ".jpg";
            case PNG -> ".png";
            case WEBP -> ".webp";
            case MP4 -> ".mp4";
            case WEBM -> ".webm";
            default -> throw new UnsupportedContentType("Cannot return postfix from content type: " + contentType);
        };
    }

    public static String parseIntoMime(ContentType contentType) throws UnsupportedContentType {
        return switch (contentType) {
            case JPEG -> "application/jpeg";
            case PNG -> "application/png";
            case WEBP -> "application/webp";
            case MP4 -> "application/mp4";
            case WEBM -> "application/webm";
            default -> throw new UnsupportedContentType("Cannot return postfix from content type: " + contentType);
        };
    }
}
