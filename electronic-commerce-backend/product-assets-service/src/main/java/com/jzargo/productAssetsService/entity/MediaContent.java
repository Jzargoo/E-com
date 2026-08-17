package com.jzargo.productAssetsService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class MediaContent {

    @Id
    private Long id;

    private String uri;

    private Long productId;

    @Builder.Default
    private Integer mediaVersion = 1;

}
