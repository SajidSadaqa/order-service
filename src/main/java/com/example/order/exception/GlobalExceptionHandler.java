package com.example.order.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {  // Removed "extends Throwable" - this was incorrect

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {
        logger.warn("Response status exception: {}", ex.getReason());
        return ResponseEntity.status(ex.getStatusCode())
                .body(createErrorResponse(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(  // Fixed return type
                                                                           MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");

        logger.warn("Validation error: {}", message);
        return ResponseEntity.badRequest()
                .body(createErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Validation failed");

        logger.warn("Constraint violation: {}", message);
        return ResponseEntity.badRequest()
                .body(createErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI()));
    }

    // Add specific business exception handlers
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFoundException(
            ProductNotFoundException ex, HttpServletRequest request) {
        logger.warn("Product not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStockException(
            InsufficientStockException ex, HttpServletRequest request) {
        logger.warn("Insufficient stock: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(createErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ProductServiceException.class)
    public ResponseEntity<Map<String, Object>> handleProductServiceException(
            ProductServiceException ex, HttpServletRequest request) {
        logger.error("Product service error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI()));
    }

    // Fixed unexpected exception handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(  // Fixed return type
                                                                           Exception ex, HttpServletRequest request) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred", request.getRequestURI()));
    }

    private Map<String, Object> createErrorResponse(HttpStatus status, String message, String path) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),  // Fixed to use status.value() instead of status
                "error", status.getReasonPhrase(),  // Fixed to use status.getReasonPhrase()
                "message", message != null ? message : "No message available",
                "path", path
        );
    }
}