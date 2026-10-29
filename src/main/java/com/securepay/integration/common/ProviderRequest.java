package com.securepay.integration.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mirror of the {@code $data} array passed to the legacy provider {@code service()} methods:
 * {@code url}, {@code method}, {@code payload}, plus arbitrary headers/extra fields.
 */
public class ProviderRequest {

    private String url;
    private String method = "POST";
    private String payload;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, Object> extra = new LinkedHashMap<>();

    public static ProviderRequest of(String url, String method, String payload) {
        ProviderRequest r = new ProviderRequest();
        r.url = url;
        r.method = method == null ? "POST" : method;
        r.payload = payload;
        return r;
    }

    public ProviderRequest header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public ProviderRequest put(String key, Object value) {
        extra.put(key, value);
        return this;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public String extraString(String key) {
        Object v = extra.get(key);
        return v == null ? null : v.toString();
    }
}
