package com.jzargo.inventory.repository;

import com.jzargo.inventory.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, String> {
}
