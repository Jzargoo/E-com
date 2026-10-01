package com.jzargo.inventory.entity;

public enum Status {
    POPULATED, // when q > 0
    DEPLETED // when q > 0 -> q = 0
}
