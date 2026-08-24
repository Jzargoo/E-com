package com.jzargo.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Version
    private Integer version;

    @Builder.Default
    private Integer quantity = 0;

    @OneToMany(targetEntity = Reservation.class, mappedBy = "inventory")
    @Builder.Default
    private List<Reservation>  reservedProducts = new ArrayList<>();

    public void updateQuantity(Integer quantity) {
        if (quantity <= 0) {
            throw new  IllegalArgumentException("Quantity must be greater than zero");
        }

        this.quantity += quantity;
    }
}

