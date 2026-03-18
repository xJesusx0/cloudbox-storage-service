package com.github.xjesusx0.cloudbox.infrastructure.api.handlers;

import com.github.xjesusx0.cloudbox.core.exceptions.CloudBoxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private ProblemDetail buildProblem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    private ProblemDetail logAndBuild(HttpStatus status, String detail, Exception ex, LogLevel level) {

        switch (level) {
            case ERROR -> log.atError().setCause(ex).log(detail);
            case WARN -> log.atWarn().setCause(ex).log(detail);
            case INFO -> log.atInfo().setCause(ex).log(detail);
        }

        return buildProblem(status, detail);
    }

    private enum LogLevel {
        ERROR, WARN, INFO
    }

    // 🔹 Excepción de negocio
    @ExceptionHandler(CloudBoxException.class)
    public ProblemDetail handleCloudBoxException(CloudBoxException ex) {
        return logAndBuild(ex.getStatus(), ex.getMessage(), ex, LogLevel.WARN);
    }

    // 🔹 Validaciones (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {

        ProblemDetail problem = logAndBuild(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                ex,
                LogLevel.INFO);

        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        problem.setProperty("errors", errors);
        return problem;
    }

    // 🔹 Parámetro faltante
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParams(MissingServletRequestParameterException ex) {
        return logAndBuild(
                HttpStatus.BAD_REQUEST,
                "Missing parameter: " + ex.getParameterName(),
                ex,
                LogLevel.INFO);
    }

    // 🔹 Tipo incorrecto
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return logAndBuild(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: " + ex.getName(),
                ex,
                LogLevel.INFO);
    }

    // 🔹 Null pointer
    @ExceptionHandler(NullPointerException.class)
    public ProblemDetail handleNullPointer(NullPointerException ex) {
        return logAndBuild(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected null value encountered",
                ex,
                LogLevel.ERROR);
    }

    // 🔹 Illegal state
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        return logAndBuild(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                ex,
                LogLevel.WARN);
    }

    // 🔹 Illegal argument
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return logAndBuild(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                ex,
                LogLevel.WARN);
    }

    // 🔹 Seguridad
    @ExceptionHandler(SecurityException.class)
    public ProblemDetail handleSecurity(SecurityException ex) {
        return logAndBuild(
                HttpStatus.FORBIDDEN,
                "Access denied",
                ex,
                LogLevel.WARN);
    }

    // 🔹 Catch-all
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        return logAndBuild(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                ex,
                LogLevel.ERROR);
    }
}