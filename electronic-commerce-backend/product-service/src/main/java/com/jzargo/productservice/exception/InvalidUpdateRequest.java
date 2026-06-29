package com.jzargo.productservice.exception;

public class InvalidUpdateRequest extends Exception {
    public InvalidUpdateRequest(String message) {
        super(message);
    }
}
