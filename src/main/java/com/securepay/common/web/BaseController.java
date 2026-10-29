package com.securepay.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Map;

/**
 * Base controller helper exposing the decrypted request payload (doc 04 §4.4).
 * Controllers can read plaintext fields via {@link #payload()} when they don't bind a typed DTO.
 */
public abstract class BaseController {

    /** The decrypted {@code {data, hash}} payload fields for the current request. */
    protected Map<String, Object> payload() {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            Object attr = request.getAttribute(DecryptedPayload.REQUEST_ATTRIBUTE);
            if (attr instanceof DecryptedPayload payload) {
                return payload.fields();
            }
        }
        return Collections.emptyMap();
    }

    protected String payloadString(String key) {
        Object v = payload().get(key);
        return v == null ? null : v.toString();
    }

    protected HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }
}
