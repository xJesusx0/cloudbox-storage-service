package com.github.xjesusx0.cloudbox.core.exceptions;

import org.springframework.http.HttpStatus;

public class FileUploadException extends CloudBoxException{
    public FileUploadException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
