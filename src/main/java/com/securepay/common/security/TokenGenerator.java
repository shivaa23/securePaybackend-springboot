package com.securepay.common.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates opaque API tokens equivalent to the source {@code Str::random(60)} (doc 07 §7.1).
 * Uses the same alphanumeric alphabet and length so stored token shape is consistent.
 */
@Component
public class TokenGenerator {

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int DEFAULT_LENGTH = 60;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
