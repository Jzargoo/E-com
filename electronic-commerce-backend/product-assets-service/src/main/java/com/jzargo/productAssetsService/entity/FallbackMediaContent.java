package com.jzargo.productAssetsService.entity;

import com.jzargo.protobuf.ContentType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table
public class FallbackMediaContent {
    @Id
    private Long queueId;

    private ContentType contentType;

    @Column("media_id")
    private String mediaUri;

    private String previousUri;

    private Long productId;

    @Builder.Default
    private Boolean isFree = true;

    @Builder.Default
    private Boolean isAvatar = false;

    private String previousMediaVersion;
}
