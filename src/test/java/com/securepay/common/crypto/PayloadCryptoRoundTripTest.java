package com.securepay.common.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Crypto parity unit tests (doc 07 §7.7): AES-128-CBC round-trip + HMAC-SHA256 hex verify.
 * These do not require a Spring context.
 */
class PayloadCryptoRoundTripTest {

    private static final String KEY = "1234567890123456"; // 16 bytes → AES-128
    private static final String IV = "6543210987654321";  // 16 bytes

    private final AesCbcCipher cipher = new AesCbcCipher();
    private final HmacUtil hmac = new HmacUtil();

    @Test
    void aesCbcRoundTrips() {
        String plaintext = "{\"username\":\"TRANS123\",\"amount\":500}";
        String encrypted = cipher.encryptToBase64(plaintext, KEY, IV);
        String decrypted = cipher.decryptFromBase64(encrypted, KEY, IV);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void hmacHexIsStableAndVerifies() {
        String data = "c29tZS1iYXNlNjQtZGF0YQ==";
        String digest = hmac.hmacSha256Hex(data, KEY);
        // 64 hex chars for SHA-256.
        assertEquals(64, digest.length());
        assertTrue(digest.chars().allMatch(c -> "0123456789abcdef".indexOf(c) >= 0));
        assertTrue(hmac.verify(data, KEY, digest));
        assertFalse(hmac.verify(data, KEY, "deadbeef"));
    }
}
