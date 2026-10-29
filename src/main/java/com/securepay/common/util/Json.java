package com.securepay.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Map;

/** Tiny static JSON helper standing in for PHP's {@code json_encode}/{@code json_decode}. */
public final class Json {
    private static final ObjectMapper OM = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private Json() {
    }

    public static String write(Object o) {
        try {
            return OM.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Decode into a Map, or null when not a JSON object. */
    public static Map<String, Object> readMap(String s) {
        if (s == null) {
            return null;
        }
        try {
            return OM.readValue(s, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return null;
        }
    }

    /** Decode into a List, or null when not a JSON array. */
    public static List<Object> readList(String s) {
        if (s == null) {
            return null;
        }
        try {
            return OM.readValue(s, new TypeReference<List<Object>>() {
            });
        } catch (Exception e) {
            return null;
        }
    }
}
