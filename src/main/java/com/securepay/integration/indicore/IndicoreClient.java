package com.securepay.integration.indicore;

import com.securepay.integration.common.ProviderHttpClient;
import com.securepay.integration.common.ProviderRequest;
import com.securepay.integration.common.ProviderResponse;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Indicore provider — port of {@code Integration/Models/Indicore.php}.
 * Form-encoded body for POST; query string for GET; returns the raw response string.
 */
@Component
public class IndicoreClient {

    private final ProviderHttpClient http;

    public IndicoreClient(ProviderHttpClient http) {
        this.http = http;
    }

    /** Port of {@code Indicore::service($url, $method, $payload)}. */
    public String service(String url, String method, Map<String, String> payload) {
        String m = method == null ? "POST" : method.toUpperCase();
        String form = formEncode(payload);

        String finalUrl = url;
        String body = null;
        if ("GET".equals(m) && payload != null && !payload.isEmpty()) {
            finalUrl = url + "?" + form;
        } else if ("POST".equals(m) && payload != null && !payload.isEmpty()) {
            body = form;
        }

        ProviderRequest req = ProviderRequest.of(finalUrl, m, body);
        if (body != null) {
            req.header("Content-Type", "application/x-www-form-urlencoded");
        }
        ProviderResponse res = http.send(req);
        return res.body();
    }

    private String formEncode(Map<String, String> payload) {
        if (payload == null) {
            return "";
        }
        return payload.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String enc(String v) {
        return v == null ? "" : URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
