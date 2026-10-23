package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@ConditionalOnBooleanProperty("database.enabled")
@Repository
public interface MessageRepository extends ReactiveCrudRepository<Message,String> {
}
