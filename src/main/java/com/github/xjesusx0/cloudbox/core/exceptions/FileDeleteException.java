package com.github.xjesusx0.cloudbox.core.exceptions;

import org.springframework.http.HttpStatus;

public class FileDeleteException extends CloudBoxException {

    private static final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

    public FileDeleteException(String message) {
        super(status, message);
    }

    public FileDeleteException(String message, Throwable cause) {
        super(status, message, cause);
    }
}
