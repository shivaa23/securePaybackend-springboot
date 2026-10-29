package com.securepay.integration.soliteck;

import com.securepay.config.SecurePayProperties;
import com.securepay.integration.common.ProviderHttpClient;
import com.securepay.integration.common.ProviderRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Soliteck payout provider — port of {@code Integration/Models/Soliteck.php}.
 *
 * <p>Flow: {@code generateToken()} (client credential headers) → use the returned token as
 * {@code ClientToken} header on the actual {@code service()} call.</p>
 */
@Component
public class SoliteckClient {

    private static final String TOKEN_URL = "https://api-uat.soliteck.in/api/Token/GenerateToken";

    private final ProviderHttpClient http;
    private final SecurePayProperties.Providers cfg;

    public SoliteckClient(ProviderHttpClient http, SecurePayProperties properties) {
        this.http = http;
        this.cfg = properties.providers();
    }

    /** Port of {@code Soliteck::generateToken()} → returns the token string or null. */
    @SuppressWarnings("unchecked")
    public String generateToken() {
        // ClientReq is a string (json-encoded twice in the source); empty here.
        String postData = "\"{\\\"ClientReq\\\":\\\"\\\"}\"";
        ProviderRequest req = ProviderRequest.of(TOKEN_URL, "POST", postData)
                .header("ClientId", nz(cfg.soliteckClientId()))
                .header("ClientPass", nz(cfg.soliteckClientPass()))
                .header("ClientTPin", nz(cfg.soliteckClientTpin()))
                .header("ClientSecret", nz(cfg.soliteckClientSecret()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");

        Map<String, Object> decoded = http.sendForJson(req);
        Object code = decoded.get("responseCode");
        Object data = decoded.get("data");
        if (code != null && "200".equals(code.toString()) && data != null) {
            return data.toString();
        }
        return null;
    }

    /** Port of {@code Soliteck::service($data)} — attaches the generated ClientToken header. */
    public Map<String, Object> service(String url, String method, String payload) {
        String token = generateToken();
        if (token == null) {
            return Map.of("status", "error", "message", "Token not generated");
        }
        ProviderRequest req = ProviderRequest.of(url, method == null ? "POST" : method, payload)
                .header("ClientId", nz(cfg.soliteckClientId()))
                .header("ClientPass", nz(cfg.soliteckClientPass()))
                .header("ClientTPin", nz(cfg.soliteckClientTpin()))
                .header("ClientSecret", nz(cfg.soliteckClientSecret()))
                .header("ClientToken", token)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");
        return http.sendForJson(req);
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}
