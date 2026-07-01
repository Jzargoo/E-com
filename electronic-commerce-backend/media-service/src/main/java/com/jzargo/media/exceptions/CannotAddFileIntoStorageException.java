package com.jzargo.media.exceptions;

import lombok.Getter;
import lombok.Setter;

@Getter
public class CannotAddFileIntoStorageException extends Exception {

    public CannotAddFileIntoStorageException(String message) {
        super(message);
    }
}
