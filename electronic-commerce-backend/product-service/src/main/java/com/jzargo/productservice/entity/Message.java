package com.jzargo.productservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Table(name = "messages")
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    @Id
    private String id;

    @Enumerated(value = EnumType.STRING)
    private MessageType messageType;

    private Instant messageCreatedTime;
}
