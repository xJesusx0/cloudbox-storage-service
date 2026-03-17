package com.github.xjesusx0.cloudbox.core.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CloudBoxException extends RuntimeException {
    private final HttpStatus status;

    public CloudBoxException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
