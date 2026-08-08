package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.ProductAssets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<ProductAssets, Long> {
}
