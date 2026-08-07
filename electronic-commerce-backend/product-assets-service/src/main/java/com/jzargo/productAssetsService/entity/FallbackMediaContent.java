package com.jzargo.productAssetsService.entity;

import jakarta.persistence.*;
import lombok.Builder;

public class FallbackMediaContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long queueId;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    @Column(
            length = 1024,
            nullable = false,
            unique = true,
            updatable = false,
            name = "media_id"
    )
    private String mediaUri;

    private Long length;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Builder.Default
    private Boolean isAvatar = false;

    @Builder.Default
    private Integer mediaVersion = 1;

    public void setProduct(Product product) {
        this.product = product;

        product.addFallbackMedia(this);
    }
}
