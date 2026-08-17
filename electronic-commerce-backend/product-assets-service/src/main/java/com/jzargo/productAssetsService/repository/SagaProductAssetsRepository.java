package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.SagaProductAssets;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SagaProductAssetsRepository extends ReactiveCrudRepository<SagaProductAssets, Long> {
}
