package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.FallbackMediaContent;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;


public interface FallbackMediaContentRepository extends R2dbcRepository<FallbackMediaContent, Integer> {
    Mono<FallbackMediaContent> findFirstByMediaUriNotNull();
}
