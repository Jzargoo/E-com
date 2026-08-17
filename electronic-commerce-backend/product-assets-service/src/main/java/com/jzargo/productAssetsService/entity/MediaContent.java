package com.jzargo.productAssetsService.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@Setter
public class MediaContent {

    @Id
    private Long id;

    private String uri;

    private Long productId;

    @Builder.Default
    private Integer mediaVersion = 1;

}
