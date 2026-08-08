package com.jzargo.productAssetsService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ProductAssets {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    private Integer shopId;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "product",referencedColumnName = "product_id", nullable = false)
    private MediaContent avatar;

    @OneToMany
    private List<MediaContent> mediaContents;

    @OneToMany
    private List<FallbackMediaContent> fallbackMediaContents;

    public void addMediaContent(MediaContent mediaContent) {

        this.mediaContents.add(mediaContent);

        mediaContent.setProduct(this);

    }

    public void addFallbackMediaContent(FallbackMediaContent fallbackMediaContent) {

        this.fallbackMediaContents.add(fallbackMediaContent);

        fallbackMediaContent.setProduct(this);
    }

    public void setAvatar(MediaContent mediaContent) {

        this.avatar = mediaContent;

        mediaContent.setProduct(this);

    }

    public void removeFallback(FallbackMediaContent fallbackMediaContent) {
        fallbackMediaContents.remove(fallbackMediaContent);

        fallbackMediaContent.setProduct(null);
    }
}