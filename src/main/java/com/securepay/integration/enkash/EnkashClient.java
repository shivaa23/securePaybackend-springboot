package com.securepay.integration.enkash;

import com.securepay.config.SecurePayProperties;
import com.securepay.integration.common.ProviderHttpClient;
import com.securepay.integration.common.ProviderRequest;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enkash penny-drop / balance provider — port of {@code Integration/Models/Enkash.php}.
 *
 * <p>Flow: OAuth password-grant token ({@code /oauth/token}, Basic auth) → Bearer token on the
 * penny-drop / balance calls.</p>
 */
@Component
public class EnkashClient {

    private static final String TOKEN_URL = "https://api.enkash.in/oauth/token";
    private static final String PENNY_DROP_URL = "https://api.enkash.in/cron/api/v0/pennyDrop";

    private final ProviderHttpClient http;
    private final SecurePayProperties.Providers cfg;

    public EnkashClient(ProviderHttpClient http, SecurePayProperties properties) {
        this.http = http;
        this.cfg = properties.providers();
    }

    /** Port of {@code Enkash::generateToken()} → access_token or null. */
    public String generateToken() {
        String body = "username=" + enc(nz(cfg.enkashUsername()))
                + "&grant_type=password"
                + "&password=" + enc(nz(cfg.enkashPassword()))
                + "&clientId=" + enc(nz(cfg.enkashClientId()));
        ProviderRequest req = ProviderRequest.of(TOKEN_URL, "POST", body)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + nz(cfg.enkashBasicAuth()));
        Map<String, Object> decoded = http.sendForJson(req);
        Object token = decoded.get("access_token");
        return token == null ? null : token.toString();
    }

    /** Port of {@code Enkash::service($data)} — penny-drop account verification with mapped result. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> pennyDrop(String beneficiaryAccount, String clientId, String ifscCode) {
        String token = generateToken();
        if (token == null) {
            return error("Failed to generate access token");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("beneficiaryAccountNumber", beneficiaryAccount);
        payload.put("customerRequestId", clientId);
        payload.put("beneficiaryIfscCode", ifscCode);
        payload.put("pennyLess", false);

        String body = json(payload);
        ProviderRequest req = ProviderRequest.of(PENNY_DROP_URL, "POST", body)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token);
        Map<String, Object> decoded = http.sendForJson(req);

        Object rc = decoded.get("response_code");
        if (rc != null && "0".equals(rc.toString())) {
            Map<String, Object> payloadResp = decoded.get("payload") instanceof Map
                    ? (Map<String, Object>) decoded.get("payload") : new HashMap<>();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("txnReferenceId", payloadResp.get("utr"));
            data.put("beneficiaryName", payloadResp.get("beneficiaryName"));
            data.put("status", payloadResp.get("status"));
            data.put("response", decoded);
            return Map.of("statuscode", "TXN",
                    "message", decoded.getOrDefault("response_message", ""),
                    "data", data);
        }
        return Map.of("statuscode", "ERR",
                "message", decoded.getOrDefault("response_message", "Verification Failed"),
                "data", Map.of("error", decoded.getOrDefault("payload", "Failed")));
    }

    private Map<String, Object> error(String message) {
        return Map.of("statuscode", "ERR", "message", message, "data", Map.of());
    }

    private String json(Map<String, Object> m) {
        return http.objectMapper() != null ? write(m) : m.toString();
    }

    private String write(Map<String, Object> m) {
        try {
            return http.objectMapper().writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}
