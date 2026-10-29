package com.securepay.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown by wallet debit when {@code w1 < debit} (doc 10 §10.3).
 * Message matches the source verbatim.
 */
public class InsufficientBalanceException extends ApiException {
    public InsufficientBalanceException() {
        super("Insufficient balance. Transaction cannot proceed.", HttpStatus.BAD_REQUEST);
    }
}
