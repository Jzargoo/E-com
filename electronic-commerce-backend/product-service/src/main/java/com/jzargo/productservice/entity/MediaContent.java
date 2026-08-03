package com.jzargo.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "media_content")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediaContent {
    @Id
    @EmbeddedId
    private MediaContentId id;

    @Builder.Default
    private Integer mediaVersion = 1;

    private boolean isAvatar;

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MediaContentId implements Serializable {

        @Column(name = "media_content")
        private String mediaContentUri;

        @ManyToOne(fetch = FetchType.LAZY)
        private Product product;
    }
}
