package com.jzargo.productservice.entity;


public enum SagaStep {
    PENDING_INVENTORY, PENDING_PRICE,
    COMPENSATE_INVENTORY, COMPENSATE_PRICE, COMPENSATE_PRODUCT,
    FINISHED, FAILED
}
