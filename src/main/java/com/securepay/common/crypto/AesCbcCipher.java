package com.securepay.common.crypto;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-128-CBC cipher for the request/response {@code {data, hash}} envelope (doc 07 §7.2, §7.3).
 *
 * <p>Parity notes:</p>
 * <ul>
 *   <li>The legacy {@code AES-128-CBC} uses PKCS#7 padding by default; the raw-data
 *       flag only affects base64 wrapping, not padding — so we use {@code AES/CBC/PKCS5Padding}.</li>
 *   <li>Key must be 16 bytes (AES-128), IV 16 bytes, both taken as raw bytes of the configured
 *       {@code USER_ENCRYPTDATA_KEY}/{@code USER_ENCRYPTDATA_IV} strings.</li>
 *   <li>The legacy {@code encrypt(..., 0, iv)} call returns a base64 string → we base64-encode the
 *       raw cipher bytes to match byte-for-byte.</li>
 * </ul>
 */
@Component
public class AesCbcCipher {

    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM = "AES";

    /**
     * Encrypt plaintext and return a base64 string (matches the legacy encrypt call with flag 0).
     */
    public String encryptToBase64(String plaintext, String key, String iv) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(key), ivSpec(iv));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cipherBytes);
        } catch (Exception e) {
            throw new CryptoException("AES-128-CBC encryption failed", e);
        }
    }

    /**
     * Decrypt a base64 ciphertext (matches the legacy raw-data decrypt call
     * where {@code data} is base64 and the decoded bytes are PKCS#7-padded).
     */
    public String decryptFromBase64(String base64CipherText, String key, String iv) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(base64CipherText);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec(key), ivSpec(iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException("AES-128-CBC decryption failed", e);
        }
    }

    private SecretKeySpec keySpec(String key) {
        return new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    private IvParameterSpec ivSpec(String iv) {
        return new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
    }
}
