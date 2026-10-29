package com.securepay.common.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.config.SecurePayProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Wallet column crypto parity (doc 07 §7.4): the {@code {iv,value,mac}} payload round-trips,
 * so a value encrypted here can be decrypted back (MAC-validated).
 */
class LegacyCryptRoundTripTest {

    private LegacyCrypt newCrypt() {
        // 32-byte key (AES-256), base64-encoded, with the "base64:" prefix like the legacy APP_KEY.
        String appKey = "base64:MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";
        SecurePayProperties props = new SecurePayProperties(
                new SecurePayProperties.Crypto(appKey, "1234567890123456", "6543210987654321"),
                new SecurePayProperties.Auth(null, null, null, List.of(), null, null),
                new SecurePayProperties.App("Live", "Asia/Kolkata"),
                null);
        return new LegacyCrypt(props, new ObjectMapper());
    }

    @Test
    void walletValueRoundTrips() {
        LegacyCrypt crypt = newCrypt();
        String stored = crypt.encryptString("500000"); // ₹5000 in paise
        String back = crypt.decryptString(stored);
        assertEquals("500000", back);
    }
}
