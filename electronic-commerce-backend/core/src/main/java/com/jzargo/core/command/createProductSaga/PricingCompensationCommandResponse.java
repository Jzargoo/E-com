package com.jzargo.core.command.createProductSaga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PricingCompensationCommandResponse implements  SagaProductCreationCommand {
    private Long productId;
    private String errorMessage;
}