package com.github.xjesusx0.cloudbox.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidParametersException extends CloudBoxException {

    public InvalidParametersException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
