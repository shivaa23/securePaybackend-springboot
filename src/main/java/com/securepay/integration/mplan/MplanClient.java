package com.securepay.integration.mplan;

import com.securepay.integration.common.ProviderHttpClient;
import com.securepay.integration.common.ProviderRequest;
import com.securepay.integration.common.ProviderResponse;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mobile plan provider — port of {@code Integration/Models/Mplan.php}.
 * Form-encoded POST / query GET; returns the raw response, or an error JSON string on failure.
 */
@Component
public class MplanClient {

    private final ProviderHttpClient http;

    public MplanClient(ProviderHttpClient http) {
        this.http = http;
    }

    /** Port of {@code Mplan::service($url, $method, $payload)}. */
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
        if (res.statusCode() == 0 || res.body() == null) {
            return "{\"statuscode\":\"ERROR\",\"message\":\"Request failed\"}";
        }
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
