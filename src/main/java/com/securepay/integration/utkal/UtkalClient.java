package com.securepay.integration.utkal;

import com.securepay.integration.common.ProviderHttpClient;
import com.securepay.integration.common.ProviderRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utkal payout/UPI provider — port of {@code Integration/Models/Utkal.php}.
 * Simple JSON POST; returns the decoded JSON map.
 */
@Component
public class UtkalClient {

    private final ProviderHttpClient http;

    public UtkalClient(ProviderHttpClient http) {
        this.http = http;
    }

    /** Port of {@code Utkal::service($data)}. */
    public Map<String, Object> service(String url, String method, String payload) {
        ProviderRequest req = ProviderRequest.of(url, method == null ? "POST" : method, payload)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        return http.sendForJson(req);
    }
}
