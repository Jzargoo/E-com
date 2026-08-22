package com.jzargo.productAssetsService.service;

import com.jzargo.productAssetsService.entity.ProductAssets;
import com.jzargo.productAssetsService.exception.ShopDoesNotOwnProductException;
import reactor.core.publisher.Mono;

public class MediaServiceValidator {

    public static Mono<ProductAssets> validateProductAssets(ProductAssets productAssets, Integer ShopId){

        if (productAssets.getShopId().equals(ShopId)){
            return Mono.just(productAssets);
        }

        return Mono.error(new ShopDoesNotOwnProductException());
    }

}
