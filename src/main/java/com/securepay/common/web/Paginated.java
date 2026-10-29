package com.securepay.common.web;

import org.springframework.data.domain.Page;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a response body that mirrors Laravel's {@code paginate()} JSON shape, so the existing
 * frontend (which reads {@code data}, {@code current_page}, {@code last_page}, {@code per_page},
 * {@code total}) keeps working unchanged.
 */
public final class Paginated {

    private Paginated() {
    }

    /** Laravel-style paginator envelope from a Spring Data {@link Page}. */
    public static Map<String, Object> of(Page<?> page) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("current_page", page.getNumber() + 1); // Laravel is 1-based
        body.put("data", page.getContent());
        body.put("from", page.getNumberOfElements() == 0 ? null : page.getNumber() * page.getSize() + 1);
        body.put("last_page", page.getTotalPages());
        body.put("per_page", page.getSize());
        body.put("to", page.getNumber() * page.getSize() + page.getNumberOfElements());
        body.put("total", page.getTotalElements());
        return body;
    }

    /** Non-paginated (export) shape: a bare list under the same contract. */
    public static List<?> of(List<?> list) {
        return list;
    }
}
