package com.securepay.modules.payout.service;

import com.securepay.common.util.Json;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Immutable view of the current request used by the payout services (the PHP {@code $request}):
 * decrypted payload fields plus caller IP and User-Agent.
 */
public record PayoutCtx(Map<String, Object> p, String userAgent, String ip) {

    public static PayoutCtx of(Map<String, Object> payload, HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        String ip = (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
        return new PayoutCtx(payload, req.getHeader("User-Agent"), ip);
    }

    /** Raw string value or null (blank counts as present, like Laravel's {@code $request->x}). */
    public String s(String key) {
        Object v = p.get(key);
        return v == null ? null : v.toString();
    }

    public String s(String key, String fallback) {
        String v = s(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    public BigDecimal amount() {
        String v = s("amount");
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.trim());
    }

    public String json() {
        return Json.write(p);
    }
}
