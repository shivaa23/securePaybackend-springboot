package com.securepay.common.security;

import com.securepay.config.SecurePayProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Peppered bcrypt for passwords/OTP/MPIN (doc 07 §7.1).
 *
 * <ul>
 *   <li>Password: {@code bcrypt.matches(password + PASS_KEY, hash)}.</li>
 *   <li>OTP: {@code bcrypt.matches(otp + OTP_KEY, hash)}.</li>
 *   <li>MPIN: {@code bcrypt.matches(mpin + PIN_KEY, hash)}.</li>
 * </ul>
 *
 * <p>The source emits {@code $2y$} hashes; the Java encoder understands {@code $2a$}. The
 * {@code $2y$}→{@code $2a$} prefixes are interchangeable for verification, so we normalize
 * before matching (doc 07 §7.1 bcrypt compatibility note).</p>
 */
@Service
public class PasswordService {

    private final PasswordEncoder encoder;
    private final String passKey;
    private final String otpKey;
    private final String pinKey;

    public PasswordService(PasswordEncoder encoder, SecurePayProperties properties) {
        this.encoder = encoder;
        SecurePayProperties.Auth auth = properties.auth();
        this.passKey = auth == null ? "" : nz(auth.passKey());
        this.otpKey = auth == null ? "" : nz(auth.otpKey());
        this.pinKey = auth == null ? "" : nz(auth.pinKey());
    }

    public boolean matchesPassword(String rawPassword, String storedHash) {
        return encoder.matches(rawPassword + passKey, normalize(storedHash));
    }

    public boolean matchesOtp(String rawOtp, String storedHash) {
        return encoder.matches(rawOtp + otpKey, normalize(storedHash));
    }

    public boolean matchesMpin(String rawMpin, String storedHash) {
        return encoder.matches(rawMpin + pinKey, normalize(storedHash));
    }

    public String hashPassword(String rawPassword) {
        return encoder.encode(rawPassword + passKey);
    }

    public String hashOtp(String rawOtp) {
        return encoder.encode(rawOtp + otpKey);
    }

    public String hashMpin(String rawMpin) {
        return encoder.encode(rawMpin + pinKey);
    }

    /** Normalize {@code $2y$} → {@code $2a$} so the Java encoder can verify source-created hashes. */
    private static String normalize(String hash) {
        if (hash == null) {
            return null;
        }
        if (hash.startsWith("$2y$")) {
            return "$2a$" + hash.substring(4);
        }
        return hash;
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}
