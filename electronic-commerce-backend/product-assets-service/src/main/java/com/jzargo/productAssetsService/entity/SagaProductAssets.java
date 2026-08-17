package com.jzargo.productAssetsService.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SagaProductAssets {

    @Id
    private Long productId;

    private Integer shopId;

    private String errorMessage;
}
