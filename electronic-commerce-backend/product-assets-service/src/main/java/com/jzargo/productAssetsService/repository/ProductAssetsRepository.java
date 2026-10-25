package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.ProductAssets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@ConditionalOnBooleanProperty("database.enabled")
public interface ProductAssetsRepository extends R2dbcRepository<ProductAssets, Long> {
    Mono<ProductAssets> findByProductIdAndShopId(Long productId, Integer shopId);

    Mono<ProductAssets> findByProductId(long l);

    Mono<Boolean> existsByProductId(Long productId);
}
