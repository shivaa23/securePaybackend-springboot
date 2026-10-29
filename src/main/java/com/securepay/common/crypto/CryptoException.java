package com.securepay.common.crypto;

/** Unchecked wrapper for low-level crypto failures. */
public class CryptoException extends RuntimeException {
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoException(String message) {
        super(message);
    }
}
