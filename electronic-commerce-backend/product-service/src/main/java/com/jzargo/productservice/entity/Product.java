package com.jzargo.productservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;


@Getter
@Setter
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
}