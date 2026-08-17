package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.Message;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends ReactiveCrudRepository<Message,String> {
}
