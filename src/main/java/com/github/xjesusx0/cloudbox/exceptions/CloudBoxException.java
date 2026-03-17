package com.github.xjesusx0.cloudbox.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class CloudBoxException extends RuntimeException {
   private final HttpStatus status;

    public CloudBoxException(HttpStatus status, String message) {
        this.status = status;
    }
}
