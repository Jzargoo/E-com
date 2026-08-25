package com.jzargo.inventory.api.exception;

import com.jzargo.inventory.exception.InventoryNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<String> handleException(Exception e) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unpredictable exception occurred in processing a request");

    }

    @ExceptionHandler
    public ResponseEntity<String> handleIAE(IllegalArgumentException e) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Incorrect arguments were provided. " + e.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<String> handleNotFound(InventoryNotFoundException e) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(e.getMessage());
    }

}
