package com.securepay.common.crypto;

import com.securepay.config.SecurePayProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the configured {@code USER_ENCRYPTDATA_KEY}/{@code USER_ENCRYPTDATA_IV} to the low-level
 * {@link AesCbcCipher} and {@link HmacUtil}, exposing the exact inbound/outbound envelope operations
 * from doc 07 §7.2–§7.3.
 */
@Component
public class PayloadCrypto {

    private final AesCbcCipher aesCbcCipher;
    private final HmacUtil hmacUtil;
    private final String key;
    private final String iv;

    public PayloadCrypto(AesCbcCipher aesCbcCipher, HmacUtil hmacUtil, SecurePayProperties properties) {
        this.aesCbcCipher = aesCbcCipher;
        this.hmacUtil = hmacUtil;
        SecurePayProperties.Crypto crypto = properties.crypto();
        this.key = crypto == null ? "" : safe(crypto.userEncryptdataKey());
        this.iv = crypto == null ? "" : safe(crypto.userEncryptdataIv());
    }

    /** Verify the HMAC of the base64 {@code data} string against {@code hash}. */
    public boolean verifyHash(String data, String hash) {
        return hmacUtil.verify(data, key, hash);
    }

    /** Compute the hex HMAC of the base64 {@code data} string (for producing envelopes if needed). */
    public String hash(String data) {
        return hmacUtil.hmacSha256Hex(data, key);
    }

    /** Decrypt an inbound base64 {@code data} payload to plaintext JSON. */
    public String decrypt(String base64Data) {
        return aesCbcCipher.decryptFromBase64(base64Data, key, iv);
    }

    /** Encrypt outbound JSON to a base64 string for {@code encrypted_data}/{@code encrypted_errors}. */
    public String encrypt(String plaintextJson) {
        return aesCbcCipher.encryptToBase64(plaintextJson, key, iv);
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
