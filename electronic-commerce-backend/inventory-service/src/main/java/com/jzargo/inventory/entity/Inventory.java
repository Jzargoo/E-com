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
    private Long productId;

    private Integer shopId;

    @Version
    private Integer version;

    @Builder.Default
    private Integer quantity = 0;

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.DEPLETED;

    @OneToMany(targetEntity = Reservation.class, mappedBy = "inventory")
    @Builder.Default
    private List<Reservation>  reservedProducts = new ArrayList<>();

    public void addQuantity(Integer quantity) {

        if (quantity <= 0) {
            throw new  IllegalArgumentException("Quantity must be greater than zero");
        } else if (this.quantity == 0) {
            this.status = Status.POPULATED;
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

        if  (this.quantity == 0) {
            this.status = Status.DEPLETED;
        }

    }
}

