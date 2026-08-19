package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.MediaContent;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MediaContentRepository extends R2dbcRepository<MediaContent, Long> {

    Flux<MediaContent> findAllByProductId(Long productId);

    Mono<MediaContent> findByUri(String uri);
}
