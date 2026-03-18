package com.github.xjesusx0.cloudbox.core.exceptions;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends CloudBoxException {
    public AuthenticationException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
