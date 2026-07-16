package com.jzargo.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "saga_product_entities")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SagaProductEntity {

    @Id
    private Long id; // PRODUCT_ID

    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private SagaStep step = SagaStep.PENDING_INVENTORY;

    @Version
    private Long version;

}
