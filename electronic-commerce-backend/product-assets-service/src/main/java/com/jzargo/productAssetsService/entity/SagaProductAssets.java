package com.jzargo.productAssetsService.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SagaProductAssets {
    @Id
    private Long productId;
    private Long shopId;

    private String errorMessage;
}
