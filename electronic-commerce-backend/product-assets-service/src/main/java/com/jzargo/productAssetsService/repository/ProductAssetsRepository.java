package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.ProductAssets;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ProductAssetsRepository extends R2dbcRepository<ProductAssets, Long> {
    Mono<ProductAssets> findByProductIdAndShopId(Long productId, Integer shopId);
}
