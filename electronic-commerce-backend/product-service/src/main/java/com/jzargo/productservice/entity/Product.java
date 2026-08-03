package com.jzargo.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Data
@Table(name = "products")
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Category category;
    @Column(nullable = false,name = "name")
    private String name;
    @Column(nullable = false,name = "description")
    private String description;
    @Column(nullable = false,name = "stock_price")
    private BigDecimal stockPrice;
    @Column(nullable = false,name = "shop_id")
    private Integer shopId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, String> characteristics = Map.of();

    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private Status status = Status.WAITING;


    @OneToMany(fetch = FetchType.LAZY, mappedBy = "id.product")
    @Builder.Default
    private List<MediaContent> mediaContent = new ArrayList<>();

    @OneToMany(targetEntity = FallbackMediaContent.class, mappedBy = "product", fetch = FetchType.LAZY)
    @Builder.Default
    private List<FallbackMediaContent> fallbackMediaContents = new ArrayList<>();

    public void addMedia(String uri) {
        MediaContent newMediaContent = MediaContent.builder()
                .id(
                        new MediaContent.MediaContentId(
                                uri, this
                        )
                )
                .build();

        mediaContent.add(newMediaContent);
    }

    public void addFallbackMedia(FallbackMediaContent fallbackMediaContent) {
        fallbackMediaContent.setProduct(this);
        fallbackMediaContents.add(fallbackMediaContent);
    }

    public void removeFallback(FallbackMediaContent fallbackMediaContent) {
        fallbackMediaContents.remove(
                fallbackMediaContent
        );

        fallbackMediaContent.setProduct(null);
    }

    public void setAvatar(String imageName) {

        this.mediaContent
                .stream()

                .filter(MediaContent::isAvatar)

                .findFirst()

                .map(media -> {

                    media.setMediaVersion(
                            media.getMediaVersion() + 1
                    );

                    media.getId().setMediaContentUri(imageName);

                    return media;
                })

                .orElseGet(
                        () -> {
                            MediaContent build = MediaContent.builder()
                                    .isAvatar(true)
                                    .id(
                                            new MediaContent.MediaContentId(
                                                    imageName, this
                                            )
                                    )
                                    .build();

                            this.mediaContent.add(build);

                            return build;
                        }
                );
    }
}