package com.securepay.integration.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Low-level HTTP client for third-party switch calls (doc 04 integration layer). Mirrors the raw
 * cURL usage of the legacy provider classes: full control over method, body and headers, JSON or
 * form bodies, configurable timeout.
 *
 * <p>Provider clients build a {@link ProviderRequest} (url/method/payload/headers) and call
 * {@link #send} or {@link #sendForJson}. HMAC/token signing is done by each provider client before
 * calling, exactly as the legacy {@code service()} methods did.</p>
 */
@Component
public class ProviderHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ProviderHttpClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /** Local-only: answer from {@link ProviderSimulator} instead of calling the live switch. */
    @org.springframework.beans.factory.annotation.Value("${securepay.providers.simulate:false}")
    private boolean simulate;

    public ProviderHttpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Send and return the raw response. */
    public ProviderResponse send(ProviderRequest request) {
        if (simulate) {
            log.info("[PROVIDER-SIMULATED] {} {}", request.getMethod(), request.getUrl());
            return new ProviderResponse(200,
                    ProviderSimulator.respond(objectMapper, request.getUrl(), request.getPayload()));
        }
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(request.getUrl()))
                    .timeout(Duration.ofSeconds(30));

            request.getHeaders().forEach(builder::header);

            String method = request.getMethod() == null ? "POST" : request.getMethod().toUpperCase();
            String payload = request.getPayload();
            HttpRequest.BodyPublisher body = payload == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8);

            switch (method) {
                case "GET" -> builder.GET();
                case "DELETE" -> builder.DELETE();
                default -> builder.method(method, body);
            }

            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new ProviderResponse(response.statusCode(), response.body());
        } catch (Exception e) {
            log.warn("Provider call failed: {} {}", request.getMethod(), request.getUrl(), e);
            return new ProviderResponse(0, null);
        }
    }

    /** Send and parse the JSON body into a {@code Map} (like {@code json_decode($response, true)}). */
    public Map<String, Object> sendForJson(ProviderRequest request) {
        ProviderResponse response = send(request);
        if (response.body() == null || response.body().isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(response.body(), new TypeReference<HashMap<String, Object>>() {
            });
        } catch (Exception e) {
            log.debug("Non-JSON provider response: {}", response.body());
            Map<String, Object> raw = new HashMap<>();
            raw.put("raw", response.body());
            return raw;
        }
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }
}
