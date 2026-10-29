package com.securepay.common.crypto;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * HMAC-SHA256 helper for the inbound envelope hash check (doc 07 §7.2).
 *
 * <p>Parity: the legacy backend computes {@code hmac_sha256($encryptedData, $secretKey)} over the
 * <b>base64 {@code data} string</b> (not the decoded bytes) with <b>lowercase hex</b> output.
 * Comparison is constant-time to avoid timing side-channels.</p>
 */
@Component
public class HmacUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** Compute lowercase-hex HMAC-SHA256 of {@code message} using {@code key}. */
    public String hmacSha256Hex(String message, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new CryptoException("HMAC-SHA256 computation failed", e);
        }
    }

    /** Constant-time comparison of the computed hex against a provided hash. */
    public boolean verify(String message, String key, String providedHash) {
        if (providedHash == null) {
            return false;
        }
        String expected = hmacSha256Hex(message, key);
        return constantTimeEquals(expected, providedHash);
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] ab = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ab.length != bb.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < ab.length; i++) {
            result |= ab[i] ^ bb[i];
        }
        return result == 0;
    }
}
