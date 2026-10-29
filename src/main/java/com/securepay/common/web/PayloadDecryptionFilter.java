package com.securepay.common.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.crypto.PayloadCrypto;
import com.securepay.common.response.ResponseFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Decrypts the inbound {@code {data, hash}} envelope for both POST (body) and GET (query), verifies
 * the HMAC, and exposes the plaintext as a {@link DecryptedPayload} request attribute plus a
 * body-wrapped request so controllers bind normal DTOs (doc 07 §7.2, doc 04 §4.4).
 *
 * <p>Runs before authentication so {@code api_token}/{@code qr_token} can be read from the
 * decrypted payload. If no envelope is present, the request passes through unchanged (so plain
 * endpoints and health checks still work).</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class PayloadDecryptionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PayloadDecryptionFilter.class);

    private final PayloadCrypto payloadCrypto;
    private final ObjectMapper objectMapper;
    private final ResponseFactory responseFactory;

    public PayloadDecryptionFilter(PayloadCrypto payloadCrypto, ObjectMapper objectMapper,
                                   ResponseFactory responseFactory) {
        this.payloadCrypto = payloadCrypto;
        this.objectMapper = objectMapper;
        this.responseFactory = responseFactory;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // multipart/form-data (e.g. fund-request receipt upload): do NOT consume the body stream —
        // let the servlet container parse the parts, then expose text fields (and image files, as
        // base64 data-URIs) as the plaintext payload so controllers read them like JSON fields.
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")
                && !"GET".equalsIgnoreCase(request.getMethod())) {
            Map<String, Object> form = new HashMap<>();
            request.getParameterMap().forEach((k, v) -> form.put(k, v.length == 1 ? v[0] : v));
            try {
                for (jakarta.servlet.http.Part part : request.getParts()) {
                    if (part.getSubmittedFileName() == null || part.getSize() == 0) {
                        continue; // plain text parts already came through getParameterMap()
                    }
                    byte[] bytes = part.getInputStream().readAllBytes();
                    String mime = part.getContentType() == null ? "image/png" : part.getContentType();
                    form.put(part.getName(), "data:" + mime + ";base64,"
                            + java.util.Base64.getEncoder().encodeToString(bytes));
                }
            } catch (Exception e) {
                log.debug("Multipart parse failed", e);
            }
            request.setAttribute(DecryptedPayload.REQUEST_ATTRIBUTE, new DecryptedPayload(form));
            filterChain.doFilter(request, response);
            return;
        }

        String data;
        String hash;
        String rawBody = null;

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            data = request.getParameter("data");
            hash = request.getParameter("hash");
        } else {
            rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            Map<String, Object> envelope = parseEnvelope(rawBody);
            data = envelope == null ? request.getParameter("data") : str(envelope.get("data"));
            hash = envelope == null ? request.getParameter("hash") : str(envelope.get("hash"));
        }

        // No envelope → treat the request as plain JSON. If the body parses as a JSON object,
        // expose its fields as the DecryptedPayload so controllers (e.g. auth/signIn) can read
        // "username"/"password"/etc. directly. This lets clients send plain JSON during local
        // testing while the encrypted {data,hash} envelope path below still works unchanged.
        if (data == null || hash == null) {
            if (rawBody != null) {
                Map<String, Object> plain = parseEnvelope(rawBody);
                if (plain != null) {
                    request.setAttribute(DecryptedPayload.REQUEST_ATTRIBUTE, new DecryptedPayload(plain));
                }
                filterChain.doFilter(new DecryptedBodyRequestWrapper(request, rawBody), response);
            } else {
                filterChain.doFilter(request, response);
            }
            return;
        }

        // 1) HMAC verify over the base64 data string (doc 07 §7.2).
        if (!payloadCrypto.verifyHash(data, hash)) {
            writeError(response, "Hash mismatch - possible tampering");
            return;
        }

        // 2) Decrypt → plaintext JSON → map.
        Map<String, Object> payload;
        try {
            String plaintext = payloadCrypto.decrypt(data);
            payload = objectMapper.readValue(plaintext, new TypeReference<HashMap<String, Object>>() {
            });
        } catch (Exception e) {
            log.debug("Payload decryption failed", e);
            writeError(response, "Decryption failed");
            return;
        }

        request.setAttribute(DecryptedPayload.REQUEST_ATTRIBUTE, new DecryptedPayload(payload));

        // 3) Replace body with plaintext JSON so DTO binding works for POST.
        String plaintextJson = objectMapper.writeValueAsString(payload);
        filterChain.doFilter(new DecryptedBodyRequestWrapper(request, plaintextJson), response);
    }

    private Map<String, Object> parseEnvelope(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawBody, new TypeReference<HashMap<String, Object>>() {
            });
        } catch (Exception e) {
            return null;
        }
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var entity = responseFactory.error(message, "");
        response.getWriter().write(objectMapper.writeValueAsString(entity.getBody()));
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
