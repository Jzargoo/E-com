package com.jzargo.productAssetsService.entity;

import com.jzargo.protobuf.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table
public class FallbackMediaContent {
    @Id
    private Long queueId;

    private ContentType contentType;

    @Column("media_id")
    private String mediaUri;

    private Long productId;

    @Builder.Default
    private Boolean isAvatar = false;

    @Builder.Default
    private Integer mediaVersion = 1;
}
