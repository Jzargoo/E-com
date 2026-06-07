package com.jzargo.productservice.entity;


public enum SagaStep {
    PENDING_INVENTORY, PENDING_PRICE, PENDING_MEDIA,
    COMPENSATE_INVENTORY, COMPENSATE_PRICE, COMPENSATE_MEDIA
}
