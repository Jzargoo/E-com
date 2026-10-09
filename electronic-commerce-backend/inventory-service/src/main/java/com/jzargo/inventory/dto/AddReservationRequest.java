package com.jzargo.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddReservationRequest {
    public Long productId;
    public Long orderId;
    public String customerId;
}
