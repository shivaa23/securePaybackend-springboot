package com.securepay.common.security;

import com.securepay.config.SecurePayProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Issues and verifies stateless JWT login tokens (HS256). Tokens are NOT stored in the database —
 * the user is resolved from the {@code sub} claim (the user id) on each request.
 *
 * <p>The secret and TTL come from {@code securepay.auth.jwt-secret} / {@code jwt-ttl-minutes}.</p>
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long ttlMinutes;

    public JwtService(SecurePayProperties properties) {
        SecurePayProperties.Auth auth = properties.auth();
        String secret = (auth == null || auth.jwtSecret() == null || auth.jwtSecret().isBlank())
                ? "securepay-default-dev-secret-please-override-0123456789"
                : auth.jwtSecret();
        // HS256 requires a key of at least 256 bits (32 bytes); pad if a short secret was configured.
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttlMinutes = (auth == null || auth.jwtTtlMinutes() == null) ? 720L : auth.jwtTtlMinutes();
    }

    /** Create a signed JWT carrying the user id (sub) and role claim. */
    public String generateToken(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttlMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /**
     * Parse and verify a JWT, returning the user id from the {@code sub} claim, or {@code null}
     * if the token is missing, malformed, expired, or has a bad signature.
     */
    public Long resolveUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return Long.valueOf(jws.getPayload().getSubject());
        } catch (Exception e) {
            // Not a valid JWT (or expired). Caller may fall back to other token schemes.
            log.debug("JWT verification failed: {}", e.getMessage());
            return null;
        }
    }
}
