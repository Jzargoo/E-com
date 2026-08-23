package com.jzargo.productservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table(name = "saga_product_entities")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SagaProductEntity {

    @Id
    private Long id; // PRODUCT_ID

    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private SagaStep step = SagaStep.PENDING_INVENTORY;

    @Version
    private Long version;

    @Column(name = "stock")
    private BigDecimal price;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "shop_id")
    private Integer shopId;

    private LocalDateTime expirationDate;

    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private SagaStatus status = SagaStatus.PROCESSING;
}
