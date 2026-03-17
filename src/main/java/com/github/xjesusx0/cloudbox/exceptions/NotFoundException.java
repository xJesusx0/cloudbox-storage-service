package com.github.xjesusx0.cloudbox.exceptions;

import org.springframework.http.HttpStatus;

public class NotFoundException extends CloudBoxException{
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
