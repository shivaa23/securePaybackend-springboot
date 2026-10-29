package com.securepay.integration.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Shared signing primitives used by the provider clients, each matching a legacy construct:
 * <ul>
 *   <li>{@link #sha256Hex} — {@code hash('sha256', $data)} (ImpsGuru generatedHash).</li>
 *   <li>{@link #sha256RawBase64} — {@code base64_encode(hash('sha256', $x, true))} (FingPay hash header).</li>
 *   <li>{@link #jwtHs256} — Paysprint JWT token.</li>
 *   <li>{@link #aes128CbcRawBase64} — FingPay request body encryption.</li>
 * </ul>
 */
@Component
public class ProviderSign {

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private final ObjectMapper objectMapper;

    public ProviderSign(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** {@code hash('sha256', $data)} → lowercase hex. */
    public String sha256Hex(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return toHex(d);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 failed", e);
        }
    }

    /** {@code base64_encode(hash('sha256', $x, true))} → base64 of raw digest bytes. */
    public String sha256RawBase64(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 failed", e);
        }
    }

    /** Paysprint JWT: HS256 over {typ:JWT,alg:HS256}.{payload}, url-safe base64, no padding. */
    public String jwtHs256(Object payload, String key) {
        try {
            String header = urlB64(objectMapper.writeValueAsBytes(
                    new java.util.LinkedHashMap<>() {{
                        put("typ", "JWT");
                        put("alg", "HS256");
                    }}));
            String body = urlB64(objectMapper.writeValueAsBytes(payload));
            String signingInput = header + "." + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            return signingInput + "." + urlB64(sig);
        } catch (Exception e) {
            throw new IllegalStateException("JWT HS256 failed", e);
        }
    }

    /** FingPay: AES-128-CBC raw-data encrypt then base64 (matches openssl_encrypt + base64_encode). */
    public String aes128CbcRawBase64(String plaintext, byte[] key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] enc = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(enc);
        } catch (Exception e) {
            throw new IllegalStateException("AES-128-CBC encrypt failed", e);
        }
    }

    private static String urlB64(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
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
}
