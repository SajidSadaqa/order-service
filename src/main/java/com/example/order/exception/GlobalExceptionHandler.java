package com.example.order.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

//partially correct, we need to cover all the exceptions (known / undefined or unexpected exception)
@RestControllerAdvice
public class GlobalExceptionHandler extends Throwable{

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(createErrorResponse(ex.getStatusCode().value(), ex.getReason(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity.badRequest()
                .body(createErrorResponse(400, message, request.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Validation failed");

        return ResponseEntity.badRequest()
                .body(createErrorResponse(400, message, request.getRequestURI()));
    }

    private Map<String, Object> createErrorResponse(int status, String message, String path) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status,
                "error", HttpStatus.valueOf(status).getReasonPhrase(),
                "message", message,
                "path", path
        );
    }
}