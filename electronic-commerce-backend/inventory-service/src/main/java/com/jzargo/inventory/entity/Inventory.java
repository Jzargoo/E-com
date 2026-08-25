package com.jzargo.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@Setter
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private Integer shopId;

    @Version
    private Integer version;

    @Builder.Default
    private Integer quantity = 0;

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @OneToMany(targetEntity = Reservation.class, mappedBy = "inventory")
    @Builder.Default
    private List<Reservation>  reservedProducts = new ArrayList<>();

    public void addQuantity(Integer quantity) {
        if (quantity <= 0) {
            throw new  IllegalArgumentException("Quantity must be greater than zero");
        }

        this.quantity += quantity;
    }

    public void removeQuantity(Integer quantity) {

        if (quantity <= 0) {
            throw new  IllegalArgumentException("Quantity must be greater than zero");
        } else if (quantity > this.quantity) {
            throw new  IllegalArgumentException("Quantity must be less than or equal to quantity");
        }

        this.quantity -= quantity;
    }



}

