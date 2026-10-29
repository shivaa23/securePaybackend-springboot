package com.securepay.common.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.config.SecurePayProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AES-256-CBC encryption for wallet columns at rest, compatible with the legacy PHP
 * {@code Crypt} payload format (doc 07 §7.4).
 *
 * <p>Payload format (what is stored in the DB column):</p>
 * <pre>
 *   base64( json( { "iv": base64(iv), "value": base64(ciphertext), "mac": hex(hmac_sha256(iv.value, key)) } ) )
 * </pre>
 *
 * <ul>
 *   <li>Cipher: AES-256-CBC, PKCS#7 padding.</li>
 *   <li>Key: {@code base64_decode(substr(APP_KEY, 7))} — the "base64:" prefix stripped.</li>
 *   <li>MAC: {@code hmac_sha256(ivB64 + valueB64, key)}, lowercase hex.</li>
 *   <li>Random 16-byte IV per encrypt.</li>
 * </ul>
 *
 * <p>This keeps stored ciphertext interoperable with the existing legacy app during any overlap.</p>
 */
@Component
public class LegacyCrypt {

    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM = "AES";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final byte[] key;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public LegacyCrypt(SecurePayProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.key = deriveKey(properties.crypto() == null ? null : properties.crypto().appKey());
    }

    private static byte[] deriveKey(String appKey) {
        if (appKey == null || appKey.isBlank()) {
            // Allow boot without a key (dev/test); failures surface lazily on use.
            return new byte[0];
        }
        String raw = appKey.startsWith("base64:") ? appKey.substring("base64:".length()) : appKey;
        return Base64.getDecoder().decode(raw);
    }

    /** Encrypt a string into the legacy payload format (base64 JSON). */
    public String encryptString(String plaintext) {
        requireKey();
        try {
            byte[] iv = new byte[16];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALGORITHM), new IvParameterSpec(iv));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            String ivB64 = Base64.getEncoder().encodeToString(iv);
            String valueB64 = Base64.getEncoder().encodeToString(cipherBytes);
            String mac = hmacHex(ivB64 + valueB64);

            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("iv", ivB64);
            payload.put("value", valueB64);
            payload.put("mac", mac);

            String json = objectMapper.writeValueAsString(payload);
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new CryptoException("Legacy Crypt encryptString failed", e);
        }
    }

    /** Decrypt a legacy payload back into the plaintext string. */
    public String decryptString(String payloadBase64) {
        requireKey();
        try {
            String json = new String(Base64.getDecoder().decode(payloadBase64), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, String> payload = objectMapper.readValue(json, Map.class);

            String ivB64 = payload.get("iv");
            String valueB64 = payload.get("value");
            String mac = payload.get("mac");

            String expectedMac = hmacHex(ivB64 + valueB64);
            if (!constantTimeEquals(expectedMac, mac)) {
                throw new CryptoException("Legacy Crypt MAC validation failed");
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ALGORITHM),
                    new IvParameterSpec(Base64.getDecoder().decode(ivB64)));
            byte[] plainBytes = cipher.doFinal(Base64.getDecoder().decode(valueB64));
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (CryptoException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CryptoException("Legacy Crypt decryptString failed", e);
        }
    }

    private void requireKey() {
        if (key.length == 0) {
            throw new CryptoException("APP_KEY is not configured; wallet Crypt cannot operate");
        }
    }

    private String hmacHex(String message) throws Exception {
        Mac macInstance = Mac.getInstance(HMAC_SHA256);
        macInstance.init(new SecretKeySpec(key, HMAC_SHA256));
        byte[] digest = macInstance.doFinal(message.getBytes(StandardCharsets.UTF_8));
        char[] out = new char[digest.length * 2];
        for (int i = 0; i < digest.length; i++) {
            int v = digest[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
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
