package com.jzargo.core;

public enum KafkaCustomHeaders {
    IDEMPOTENCY_KEY("idempotent-messageId-key"),
    SAGA_ID_KEY("sagaId-key");

    private final String value;

    KafkaCustomHeaders(String s) {
        this.value = s;
    }

    public String getValue() {
        return value;
    }
}
