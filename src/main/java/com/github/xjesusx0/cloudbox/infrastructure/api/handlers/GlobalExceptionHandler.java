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

    // 🔹 Método helper para no repetir código
    private ProblemDetail buildProblem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // 🔹 Excepción de negocio
    @ExceptionHandler(CloudBoxException.class)
    public ProblemDetail handleCloudBoxException(CloudBoxException ex) {
        return buildProblem(ex.getStatus(), ex.getMessage());
    }

    // 🔹 Validaciones (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {

        ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, "Validation failed");

        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        problem.setProperty("errors", errors);
        return problem;
    }

    // 🔹 Parámetro faltante
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParams(MissingServletRequestParameterException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST,
                "Missing parameter: " + ex.getParameterName());
    }

    // 🔹 Tipo incorrecto (ej: enum mal enviado)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: " + ex.getName());
    }

    // 🔹 Null pointer (error de backend)
    @ExceptionHandler(NullPointerException.class)
    public ProblemDetail handleNullPointer(NullPointerException ex) {
        log.error("NullPointerException", ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected null value encountered");
    }

    // 🔹 Illegal state (estado inválido)
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        log.warn("IllegalStateException", ex);
        return buildProblem(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 🔹 Illegal argument (input inválido)
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 🔹 Seguridad (opcional si luego usas Spring Security)
    @ExceptionHandler(SecurityException.class)
    public ProblemDetail handleSecurity(SecurityException ex) {
        return buildProblem(HttpStatus.FORBIDDEN, "Access denied");
    }

    // 🔹 Catch-all
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }
}