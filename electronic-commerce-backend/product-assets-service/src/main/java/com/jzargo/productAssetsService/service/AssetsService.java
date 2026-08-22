package com.jzargo.productAssetsService.service;

import com.jzargo.productAssetsService.entity.ProductAssets;
import reactor.core.publisher.Mono;

public interface AssetsService {
    Mono<Void> initAssetsCompensation(Long productId);
    Mono<ProductAssets> initAssetsProduct(Long productId, Integer shopId);
}
