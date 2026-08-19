package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.FallbackMediaContent;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;


public interface FallbackMediaContentRepository extends R2dbcRepository<FallbackMediaContent, Long> {
    Mono<FallbackMediaContent> findFirstByIsFreeIsTrue();

    @Modifying
    @Query("UPDATE fallback_media_content SET fallback_media_content.is_free=false WHERE fallback_media_content.queue_id = :id AND fallback_media_content.is_free=true")
    Mono<Integer> lockProcessing(Long id);
}
