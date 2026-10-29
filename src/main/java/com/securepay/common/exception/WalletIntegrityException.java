package com.securepay.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown by wallet debit post-check when {@code w1 < 0} (doc 10 §10.3).
 * Message matches the source verbatim; triggers transaction rollback.
 */
public class WalletIntegrityException extends ApiException {
    public WalletIntegrityException() {
        super("Wallet integrity error: negative balance detected. Transaction rolled back.",
                HttpStatus.BAD_REQUEST);
    }
}
