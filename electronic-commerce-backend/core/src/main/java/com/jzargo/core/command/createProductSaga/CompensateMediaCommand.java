package com.jzargo.core.command.createProductSaga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompensateMediaCommand {
    private Long productId;
}
