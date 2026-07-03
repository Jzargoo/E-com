package com.jzargo.productservice.entity;

import com.jzargo.protobuf.ContentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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

    @Builder.Default
    private String mediaId = UUID.randomUUID().toString();

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Builder.Default
    private Boolean isAvatar = false;

    public void setProduct(Product product) {
        this.product = product;

        product.addFallbackMedia(this);
    }
}
