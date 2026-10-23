package com.jzargo.productAssetsService.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("product_avatar_mapping")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Avatar {
    @Id
    private Long id;

    private Long productId;

    private Long contentId;
}