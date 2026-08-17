package com.jzargo.productAssetsService.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


@Table
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ProductAssets {

    @Id
    private Long productId;

    private Integer shopId;

    private MediaContent avatar;
}