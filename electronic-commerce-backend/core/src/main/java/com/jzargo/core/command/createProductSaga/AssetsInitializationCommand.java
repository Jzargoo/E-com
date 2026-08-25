package com.jzargo.core.command.createProductSaga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetsInitializationCommand implements  SagaProductCreationCommand {
    private Long productId;
    private Integer shopId;
}
