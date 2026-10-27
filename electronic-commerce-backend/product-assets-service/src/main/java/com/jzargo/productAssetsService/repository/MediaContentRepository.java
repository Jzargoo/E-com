package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.MediaContent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ConditionalOnBooleanProperty("database.enabled")
@Repository
public interface MediaContentRepository extends R2dbcRepository<MediaContent, Long> {

    Mono<MediaContent> findByUri(String uri);


    @Modifying
    @Query("UPDATE media_content SET media_version=:version WHERE id=:id")
    Mono<Integer> updateVersion(Long id, String version);

    Mono<Boolean> existsByProductAssetsId(Long productAssetsId);

    Flux<MediaContent> findAllByProductAssetsId(Long id);

    Mono<MediaContent> findByProductAssetsId(Long productAssetsId);
}
