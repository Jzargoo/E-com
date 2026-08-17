package com.jzargo.productAssetsService.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("product_avatar_mapping")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Avatar {
    @Id
    private Long productId;

    @Column("avatar_id")
    private Long contentId;
}