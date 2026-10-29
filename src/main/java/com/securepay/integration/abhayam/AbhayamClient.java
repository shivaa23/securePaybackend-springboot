package com.securepay.integration.abhayam;

import com.securepay.integration.common.ProviderHttpClient;
import com.securepay.integration.common.ProviderRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Abhayam payout provider — port of {@code Integration/Models/Abhayam.php}.
 * Simple JSON POST with Content-Type/Accept headers; returns the decoded JSON map.
 */
@Component
public class AbhayamClient {

    private final ProviderHttpClient http;

    public AbhayamClient(ProviderHttpClient http) {
        this.http = http;
    }

    /** Port of {@code Abhayam::service($data)}. */
    public Map<String, Object> service(String url, String method, String payload) {
        ProviderRequest req = ProviderRequest.of(url, method == null ? "POST" : method, payload)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        return http.sendForJson(req);
    }
}
