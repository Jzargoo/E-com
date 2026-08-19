package com.jzargo.productAssetsService.service;

import com.jzargo.productAssetsService.entity.MediaContent;
import com.jzargo.productAssetsService.entity.ProductAssets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MediaServiceLogger {
    public static void logFoundAsset(ProductAssets asset){
        log.trace("caught an product asset with content: {}", asset);
    }

    public static void logStartingExecuting(String methodName){
        log.debug("{} started executing.", methodName);
    }

    public static void logShopDoesNotOwn(Integer shopId, Long productId){
        log.error("Shop {} does not own a product {} to change avatar . Aborting with an exception", shopId, productId);
    }

    public static void logException(Throwable cause, String action){
        log.error(
                "Occurred exception {} with message {} while {}",
                cause.getClass(), cause.getMessage(),action, cause
        );
    }

    public static void logFoundMediaContent(MediaContent mediaContent) {
        log.trace("Caught media content with content {}", mediaContent);
    }

}
