package com.smartretailx.inventory.exception;

import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        Map<String, String> validationErrors
    ) {}

    @ExceptionHandler(InventoryExceptions.ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(
            InventoryExceptions.ProductNotFoundException ex) {

        return buildErrorResponse(HttpStatus.NOT_FOUND, "Product Not Found", ex.getMessage());
    }

    @ExceptionHandler(InventoryExceptions.InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InventoryExceptions.InsufficientStockException ex) {

        return buildErrorResponse(HttpStatus.CONFLICT, "Insufficient Stock", ex.getMessage());
    }

    @ExceptionHandler(InventoryExceptions.DuplicateSkuException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSku(
            InventoryExceptions.DuplicateSkuException ex) {

        return buildErrorResponse(HttpStatus.CONFLICT, "Duplicate SKU", ex.getMessage());
    }

    @ExceptionHandler(InventoryExceptions.InvalidStockOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(
            InventoryExceptions.InvalidStockOperationException ex) {

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Operation", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "One or more fields have invalid values",
            LocalDateTime.now(),
            fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred. Please contact support."
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, String error, String message) {

        ErrorResponse response = new ErrorResponse(
            status.value(),
            error,
            message,
            LocalDateTime.now(),
            Collections.emptyMap()
        );
        return ResponseEntity.status(status).body(response);
    }
}
