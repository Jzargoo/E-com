package com.jzargo.productAssetsService.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "messages")
public class Message {

    @Id
    private Long id;

    private String messageId;
    @Builder.Default
    private Instant processedAt =  Instant.now();

    private MessageType messageType;
}
