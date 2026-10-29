package com.securepay.common.security;

import com.securepay.common.web.PayloadDecryptionFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless, token-based security (doc 07 §7.1).
 *
 * <ul>
 *   <li>CSRF disabled (token/encryption-based), sessions STATELESS.</li>
 *   <li>Public ("except") endpoints: login/QR + health + docs.</li>
 *   <li>All other endpoints require an authenticated {@link CurrentUser}.</li>
 *   <li>401 entry point returns the error envelope, not Spring's default.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/auth/signIn",
            "/auth/secureLogin",
            "/auth/getMobile",
            "/auth/forgetPassword",
            "/auth/resetMpin",
            "/auth/resendOtp",
            "/auth/generateQr",
            "/auth/qrStatus",
            "/auth/approveQr",
            "/signup/**",
            "/webhook/**",
            // Receipt images are loaded by <img src> (no Authorization header); the PHP route is public too.
            "/secure-images/**",
            "/fundrequest/secure-images/**",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final TokenAuthenticationFilter tokenAuthenticationFilter;
    private final PayloadDecryptionFilter payloadDecryptionFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(TokenAuthenticationFilter tokenAuthenticationFilter,
                          PayloadDecryptionFilter payloadDecryptionFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
        this.payloadDecryptionFilter = payloadDecryptionFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh.authenticationEntryPoint(authenticationEntryPoint))
                // Decrypt the {data,hash} envelope (and expose plain JSON) BEFORE resolving the
                // token, since api_token may come from the decrypted payload. Both run inside the
                // Security chain so the authentication they set actually sticks.
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(payloadDecryptionFilter, tokenAuthenticationFilter.getClass());
        return http.build();
    }

    /**
     * BCrypt matches the legacy bcrypt. The source emits {@code $2y$} hashes; this encoder emits
     * {@code $2a$}. Normalize the prefix before matching (doc 07 §7.1) — handled in the auth service.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
