package com.securepay.modules.auth.repository;

import com.securepay.modules.auth.entity.QrSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/** Repository for {@link QrSession} (QR web-login, doc 07 §7.1). */
public interface QrSessionRepository extends JpaRepository<QrSession, Long> {

    Optional<QrSession> findByToken(String token);

    /** Approved and not-expired session for a given token (doc 07 §7.1 qr_token fallback). */
    Optional<QrSession> findByTokenAndStatusAndExpiresAtAfter(String token, String status, LocalDateTime now);
}
