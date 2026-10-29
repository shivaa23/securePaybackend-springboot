package com.securepay.integration.common;

/** Raw provider HTTP response: status code + body string. */
public record ProviderResponse(int statusCode, String body) {

    public boolean isOk() {
        return statusCode >= 200 && statusCode < 300;
    }
}
