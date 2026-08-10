package com.jzargo.productAssetsService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "messages")
public class Message {
    @Id
    private String messageId;

    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;
}
