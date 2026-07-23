package com.jzargo.productservice.helper;

import com.jzargo.core.command.createProductSaga.*;
import com.jzargo.productservice.entity.SagaStep;

import java.math.BigDecimal;
import java.util.Map;

public class DebeziumMessageParser {
    private DebeziumMessageParser() {}

    @SuppressWarnings("unchecked")
    public static String getOperationByRoot(Map<String, Object> root) {
        var payload = (Map<String, Object>) root.get("payload");

        return (String) payload.get("op");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getAfterByRoot(Map<String, Object> root) {
        var payload = (Map<String, Object>) root.get("payload");

        return (Map<String, Object>) payload.get("after");
    }


    public static Object getSagaCreateCommandByAfter(Map<String, Object> after) {

        String step = (String) after.get("step");

        Number nid = (Number) after.get("id");

        Long id = nid.longValue();

        return switch (SagaStep.valueOf(step)) {


            case PENDING_INVENTORY ->
                    new InventoryCommand(id);


            case PENDING_PRICE -> {
                BigDecimal price = (BigDecimal) after.get("price");

                yield new PricingCommand(
                        id,
                        price.doubleValue()
                );
            }


            case COMPENSATE_INVENTORY ->
                    new CompensateInventoryCommand(id);


            case COMPENSATE_PRODUCT ->
                    new CompensateProductCommand(id);


            case COMPENSATE_PRICE ->
                    new CompensatePricingCommand(id);


            case FAILED -> {
                String message =
                        (String) after.getOrDefault("error_message", "");

                yield new FailCreateProductCreation(id, message);
            }

            case FINISHED ->
                    new FinishCreateProductCreation(id);

        };
    }
}
