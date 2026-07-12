package com.jzargo.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;


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

    @Column(nullable = false,name = "avatar")
    private String avatar;

    @ManyToOne
    private Category category;
    @Column(nullable = false,name = "name")
    private String name;
    @Column(nullable = false,name = "description")
    private String description;
    @Column(nullable = false,name = "stock_price")
    private Double stockPrice;
    @Column(nullable = false,name = "shop_id")
    private Integer shopId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, String> characteristics = Map.of();

    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private Status status = Status.WAITING;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "media_content",
            joinColumns = {
                    @JoinColumn(name = "product_id", referencedColumnName = "id")
            }
    )
    @Column(name = "media_content")
    @Builder.Default
    private List<String> mediaContent = new ArrayList<>();

    @OneToMany(targetEntity = FallbackMediaContent.class, mappedBy = "product", fetch = FetchType.LAZY)
    @Builder.Default
    private List<FallbackMediaContent> fallbackMediaContents = new ArrayList<>();

    public void addMedia(List<String> filesId) {
        mediaContent.addAll(filesId);
    }

    public void addFallbackMedia(FallbackMediaContent fallbackMediaContent) {
        fallbackMediaContents.add(fallbackMediaContent);
    }
}