package com.jzargo.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FallbackMediaContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long queueId;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    private String mediaId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
