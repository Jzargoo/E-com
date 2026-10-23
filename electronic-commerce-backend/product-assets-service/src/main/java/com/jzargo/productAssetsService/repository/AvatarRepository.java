package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.Avatar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@ConditionalOnBooleanProperty("database.enabled")
@Repository
public interface AvatarRepository extends R2dbcRepository<Avatar,Long> {
    Mono<Avatar> findByProductId(Long productId);
}
