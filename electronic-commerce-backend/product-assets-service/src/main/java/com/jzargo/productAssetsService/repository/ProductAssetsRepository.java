package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.ProductAssets;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAssetsRepository extends JpaRepository<ProductAssets, Long> {
}
