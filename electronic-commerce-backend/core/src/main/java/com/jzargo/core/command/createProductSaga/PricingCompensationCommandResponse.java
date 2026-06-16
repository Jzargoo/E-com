package com.jzargo.core.command.createProductSaga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PricingCompensationCommandResponse {
    private Long productId;
}