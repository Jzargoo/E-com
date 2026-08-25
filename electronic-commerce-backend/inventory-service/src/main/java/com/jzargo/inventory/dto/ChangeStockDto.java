package com.jzargo.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeStockDto {
    @NotNull
    @Min(value = 1, message = "Quantity has to be greater than zero")
    private Integer quantity;
    @NotNull
    @Min(value = 1, message = "product id cannot be less than zero")
    private Long productId;
}
