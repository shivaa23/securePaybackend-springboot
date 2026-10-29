package com.securepay.modules.auth.service;

import com.securepay.common.audit.AuditLogService;
import com.securepay.common.security.PasswordService;
import com.securepay.common.security.TokenGenerator;
import com.securepay.config.SecurePayProperties;
import com.securepay.modules.auth.entity.QrSession;
import com.securepay.modules.auth.repository.QrSessionRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port of {@code AuthController} login/QR logic (doc 07 §7.1).
 *
 * <ul>
 *   <li>signIn: UA allow-list, username must start with {@code TRANS}, numeric id = substr(5),
 *       peppered bcrypt password check, audit write.</li>
 *   <li>secureLogin: re-check UA + OTP/MPIN, return existing api_token (no rotation).</li>
 *   <li>QR web login: generateQr / qrStatus / approveQr with {@link QrSession}.</li>
 * </ul>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final QrSessionRepository qrSessionRepository;
    private final PasswordService passwordService;
    private final TokenGenerator tokenGenerator;
    private final AuditLogService auditLogService;
    private final List<String> allowedUserAgents;

    public AuthService(UserRepository userRepository,
                       QrSessionRepository qrSessionRepository,
                       PasswordService passwordService,
                       TokenGenerator tokenGenerator,
                       AuditLogService auditLogService,
                       SecurePayProperties properties) {
        this.userRepository = userRepository;
        this.qrSessionRepository = qrSessionRepository;
        this.passwordService = passwordService;
        this.tokenGenerator = tokenGenerator;
        this.auditLogService = auditLogService;
        SecurePayProperties.Auth auth = properties.auth();
        this.allowedUserAgents = auth != null && auth.allowedUserAgents() != null
                ? auth.allowedUserAgents()
                : List.of("Transup/1.0", "ImpsguruAppWeb/1.0", "Mozilla");
    }

    /** The source allow-list: reject Postman/curl; accept Transup/ImpsguruAppWeb/Mozilla. */
    public boolean isAllowedUserAgent(String userAgent) {
        if (userAgent == null) {
            return false;
        }
        return allowedUserAgents.stream().anyMatch(userAgent::contains);
    }

    /** Resolve the numeric user id from a {@code TRANS...} username (substr from index 5). */
    public Optional<User> resolveByUsername(String username) {
        if (username == null || !username.startsWith("TRANS")) {
            return Optional.empty();
        }
        String idPart = username.substring(5);
        try {
            return userRepository.findById(Long.parseLong(idPart));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordService.matchesPassword(rawPassword, user.getPassword());
    }

    public boolean checkMpin(User user, String rawMpin) {
        return passwordService.matchesMpin(rawMpin, user.getMpin());
    }

    public void audit(Long userId, String role, String action, String request,
                      String response, String ip, String ua, String status) {
        auditLogService.record(userId, role, action, "auth", request, response, ip, ua, status);
    }

    // ------------------------------------------------------------------
    // QR web login (doc 07 §7.1)
    // ------------------------------------------------------------------

    /** generateQr: create a pending session valid for 2 minutes; return the token. */
    public String generateQr() {
        String token = UUID.randomUUID().toString();
        QrSession qr = new QrSession();
        qr.setToken(token);
        qr.setStatus("pending");
        qr.setExpiresAt(LocalDateTime.now().plusMinutes(2));
        qrSessionRepository.save(qr);
        return token;
    }

    public Optional<QrSession> findLiveSession(String token) {
        return qrSessionRepository.findByToken(token)
                .filter(q -> q.getExpiresAt() != null && q.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    /** approveQr: mark the pending session approved for the given user; rotate token unless role api. */
    public void approveQr(QrSession qr, User user) {
        if (!"api".equals(user.getRole())) {
            user.setApiToken(tokenGenerator.generate());
            userRepository.save(user);
        }
        qr.setStatus("approved");
        qr.setUserId(user.getId());
        qrSessionRepository.save(qr);
    }
}
