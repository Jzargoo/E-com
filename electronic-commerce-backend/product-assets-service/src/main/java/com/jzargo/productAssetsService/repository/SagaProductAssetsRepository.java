package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.SagaProductAssets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@ConditionalOnBooleanProperty("database.enabled")
@Repository
public interface SagaProductAssetsRepository extends ReactiveCrudRepository<SagaProductAssets, Long> {
}
