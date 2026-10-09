package com.jzargo.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Reservation{

    @Id
    private Long orderId;

    @ManyToOne
    private Inventory inventory;

    @Builder.Default
    private Integer quantity = 1;

    private String customerId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ReservationStatus status =  ReservationStatus.WAITING;
}