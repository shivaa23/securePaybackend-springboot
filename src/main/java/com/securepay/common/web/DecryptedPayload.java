package com.securepay.common.web;

import java.util.Collections;
import java.util.Map;

/**
 * Holds the decrypted {@code {data, hash}} payload for the current request, exposed as a request
 * attribute so downstream filters/controllers can read plaintext fields (mirrors the source
 * {@code $request->replace($payload)} behaviour, doc 04 §4.4).
 */
public final class DecryptedPayload {

    public static final String REQUEST_ATTRIBUTE = "securepay.decryptedPayload";

    private final Map<String, Object> fields;

    public DecryptedPayload(Map<String, Object> fields) {
        this.fields = fields == null ? Collections.emptyMap() : fields;
    }

    public Map<String, Object> fields() {
        return fields;
    }

    public Object get(String key) {
        return fields.get(key);
    }

    public String getString(String key) {
        Object v = fields.get(key);
        return v == null ? null : v.toString();
    }
}
