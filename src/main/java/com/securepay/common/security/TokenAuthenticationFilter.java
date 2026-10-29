package com.securepay.common.security;

import com.securepay.common.web.DecryptedPayload;
import com.securepay.modules.auth.entity.QrSession;
import com.securepay.modules.auth.repository.QrSessionRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Resolves the current user from an opaque token (doc 07 §7.1), token precedence:
 * <ol>
 *   <li>{@code Authorization: Bearer <token>} header</li>
 *   <li>{@code api_token} request input (from decrypted payload)</li>
 *   <li>{@code qr_token} → approved, non-expired {@link QrSession} → user</li>
 * </ol>
 * Sets a {@link CurrentUser} principal in the {@link SecurityContextHolder} on success.
 * Does not reject here — {@code SecurityConfig} enforces auth requirements (401 via entry point).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final QrSessionRepository qrSessionRepository;
    private final JwtService jwtService;

    public TokenAuthenticationFilter(UserRepository userRepository, QrSessionRepository qrSessionRepository,
                                     JwtService jwtService) {
        this.userRepository = userRepository;
        this.qrSessionRepository = qrSessionRepository;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveUser(request).ifPresent(user -> {
                CurrentUser principal = new CurrentUser(user);
                var authorities = List.of(new SimpleGrantedAuthority(
                        "ROLE_" + (user.getRole() == null ? "USER" : user.getRole().toUpperCase())));
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        filterChain.doFilter(request, response);
    }

    private Optional<User> resolveUser(HttpServletRequest request) {
        // Token can arrive as a Bearer header or as api_token (decrypted payload / query param).
        // The frontend sends the login token back as api_token, so both carry the JWT now.
        String token = bearerToken(request);
        if (token == null) {
            token = payloadString(request, "api_token");
        }

        if (token != null) {
            // 1) Preferred: stateless JWT → user id from the `sub` claim (nothing in the DB).
            Long userId = jwtService.resolveUserId(token);
            if (userId != null) {
                return userRepository.findById(userId);
            }
            // 2) Backward-compat: any legacy opaque api_token still stored on a user row.
            Optional<User> byApiToken = userRepository.findByApiToken(token);
            if (byApiToken.isPresent()) {
                return byApiToken;
            }
        }

        // 3) qr_token → approved, non-expired QrSession → user
        String qrToken = payloadString(request, "qr_token");
        if (qrToken != null) {
            Optional<QrSession> session = qrSessionRepository
                    .findByTokenAndStatusAndExpiresAtAfter(qrToken, "approved", LocalDateTime.now());
            if (session.isPresent() && session.get().getUserId() != null) {
                return userRepository.findById(session.get().getUserId());
            }
        }
        return Optional.empty();
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String t = header.substring(7).trim();
            return t.isEmpty() ? null : t;
        }
        return null;
    }

    private String payloadString(HttpServletRequest request, String key) {
        Object attr = request.getAttribute(DecryptedPayload.REQUEST_ATTRIBUTE);
        if (attr instanceof DecryptedPayload payload) {
            String v = payload.getString(key);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        String param = request.getParameter(key);
        return (param != null && !param.isBlank()) ? param : null;
    }
}
