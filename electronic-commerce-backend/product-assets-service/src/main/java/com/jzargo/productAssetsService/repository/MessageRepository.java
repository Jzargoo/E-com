package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ConditionalOnBooleanProperty("database.enabled")
@Repository
public interface MessageRepository extends ReactiveCrudRepository<Message,String> {

    Mono<Boolean> existsByMessageId(String messageId);
}
