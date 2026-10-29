package com.securepay.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Business exception that maps to the error envelope (doc 08 §8.5).
 * Carries the exact message + HTTP status the source surfaced to clients.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Object errors;

    public ApiException(String message) {
        this(message, HttpStatus.BAD_REQUEST, "");
    }

    public ApiException(String message, HttpStatus status) {
        this(message, status, "");
    }

    public ApiException(String message, HttpStatus status, Object errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object getErrors() {
        return errors;
    }
}
