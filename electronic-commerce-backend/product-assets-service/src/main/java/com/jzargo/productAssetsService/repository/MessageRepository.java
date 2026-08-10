package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message,String> {
}
