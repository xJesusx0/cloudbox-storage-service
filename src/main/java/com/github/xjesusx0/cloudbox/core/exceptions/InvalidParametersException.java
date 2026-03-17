package com.github.xjesusx0.cloudbox.core.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidParametersException extends CloudBoxException {

    public InvalidParametersException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
