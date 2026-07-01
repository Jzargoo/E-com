package com.jzargo.media.exceptions;

import lombok.Getter;
import lombok.Setter;

public class CannotAddFileIntoStorageException extends Exception {

    public CannotAddFileIntoStorageException(String message) {
        super(message);
    }
}
