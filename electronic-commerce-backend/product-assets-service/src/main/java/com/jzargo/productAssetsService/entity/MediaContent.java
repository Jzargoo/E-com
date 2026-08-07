package com.jzargo.productAssetsService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class MediaContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 128, nullable = false, unique = true)
    private String uri;

    @Builder.Default
    private Integer mediaVersion = 1;

}
