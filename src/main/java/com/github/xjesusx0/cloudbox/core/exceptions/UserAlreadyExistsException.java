package com.github.xjesusx0.cloudbox.core.exceptions;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends CloudBoxException {
    public UserAlreadyExistsException(String username) {
        super(HttpStatus.CONFLICT, "Username '" + username + "' is already taken");
    }
}
