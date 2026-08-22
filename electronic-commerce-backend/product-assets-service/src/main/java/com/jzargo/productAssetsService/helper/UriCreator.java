package com.jzargo.productAssetsService.helper;

import com.jzargo.productAssetsService.exception.UnsupportedContentType;
import com.jzargo.protobuf.ContentType;

import java.util.UUID;

public class UriCreator {

    public static String getUniqueUriByProductIdAndContentType(Long productId, ContentType contentType)
            throws UnsupportedContentType {

        return "products/%s/%s.%s"
                .formatted(
                        productId,

                        UUID.randomUUID().toString()
                                .replace(".", ""),

                        ContentTypeParser.getMediaPostfix(contentType)
                );

    }
}
