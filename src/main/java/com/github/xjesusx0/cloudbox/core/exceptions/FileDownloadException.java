package com.github.xjesusx0.cloudbox.core.exceptions;

import org.springframework.http.HttpStatus;

public class FileDownloadException extends CloudBoxException{

    private static final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

    public FileDownloadException(String message) {
        super(status, message);
    }

    public FileDownloadException(String message, Throwable cause) {
        super(status, message, cause);
    }
}
