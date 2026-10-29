package com.securepay.common.exception;

import com.securepay.common.response.ResponseFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps exceptions to the error envelope so clients always see the same error shape (doc 08 §8.5).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ResponseFactory responseFactory;

    public GlobalExceptionHandler(ResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    /** Business exceptions carry their own message + status. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return responseFactory.error(ex.getMessage(), ex.getErrors(), ex.getStatus());
    }

    /** Bean Validation on @RequestBody DTOs → 400 error envelope with per-field messages. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return responseFactory.error("Validation failed", errors, HttpStatus.BAD_REQUEST);
    }

    /** Bean Validation on @RequestParam/@PathVariable → 400 error envelope. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException ex) {
        Map<String, Object> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            errors.put(v.getPropertyPath().toString(), v.getMessage());
        }
        return responseFactory.error("Validation failed", errors, HttpStatus.BAD_REQUEST);
    }

    /** Uncaught → generic error envelope (matches the legacy "Something went wrong"). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return responseFactory.error("Something went wrong", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
