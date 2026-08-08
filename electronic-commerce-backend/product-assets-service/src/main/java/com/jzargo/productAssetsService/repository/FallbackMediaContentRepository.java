package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.FallbackMediaContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FallbackMediaContentRepository extends JpaRepository <FallbackMediaContent, Integer> {
    Optional<FallbackMediaContent> findFirstByMediaUriIsNotNull();
}
