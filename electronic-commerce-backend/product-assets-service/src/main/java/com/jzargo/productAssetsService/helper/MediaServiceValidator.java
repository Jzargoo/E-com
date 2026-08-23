package com.jzargo.productAssetsService.helper;

import com.jzargo.productAssetsService.entity.ProductAssets;
import com.jzargo.productAssetsService.exception.ShopDoesNotOwnProductException;
import reactor.core.publisher.Mono;

public class MediaServiceValidator {

    public static Mono<ProductAssets> validateProductAssets(ProductAssets productAssets, Integer shopId){

        if (productAssets.getShopId().equals(shopId)){
            return Mono.just(productAssets);
        }

        GlobalLogger.logShopDoesNotOwn(shopId, productAssets.getProductId());

        return Mono.error(new ShopDoesNotOwnProductException());
    }

}
