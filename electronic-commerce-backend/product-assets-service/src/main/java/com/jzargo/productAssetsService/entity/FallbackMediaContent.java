package com.jzargo.productAssetsService.entity;

import com.jzargo.protobuf.ContentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table
@Entity
public class FallbackMediaContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long queueId;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    @Column(
            length = 128,
            nullable = false,
            unique = true,
            updatable = false,
            name = "media_id"
    )
    private String mediaUri;


    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductAssets product;

    @Builder.Default
    private Boolean isAvatar = false;

    @Builder.Default
    private Integer mediaVersion = 1;
}
