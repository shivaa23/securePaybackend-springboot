package com.securepay.common.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Small Laravel-style request validator. Mirrors the semantics the legacy {@code Validator::make}
 * rules rely on: a field that is absent/blank skips every rule except {@code required}; {@code
 * sometimes} skips an absent field entirely; {@code min}/{@code max}/{@code between} compare
 * numerically when a {@code numeric}/{@code integer} rule is present, else by string length.
 * Errors are returned as {@code {field: [messages]}} (Laravel's error bag shape).
 */
public final class Validation {

    private final Map<String, Object> data;
    private final Map<String, List<String>> errors = new LinkedHashMap<>();
    private final Map<String, String> custom = new HashMap<>();

    private Validation(Map<String, Object> data) {
        this.data = data == null ? Map.of() : data;
    }

    public static Validation of(Map<String, Object> data) {
        return new Validation(data);
    }

    /** Custom messages keyed {@code "field.rule"} (e.g. {@code "client_ref.min"}). */
    public Validation messages(Map<String, String> messages) {
        custom.putAll(messages);
        return this;
    }

    public Field field(String name) {
        return new Field(name);
    }

    public boolean fails() {
        return !errors.isEmpty();
    }

    public Map<String, List<String>> errors() {
        return errors;
    }

    private String value(String name) {
        Object v = data.get(name);
        return v == null ? null : v.toString();
    }

    private void add(String field, String rule, String defaultMessage) {
        String msg = custom.getOrDefault(field + "." + rule, defaultMessage);
        errors.computeIfAbsent(field, k -> new ArrayList<>()).add(msg);
    }

    /** One field's rule chain. */
    public final class Field {
        private final String name;
        private final String label;
        private final String raw;
        private boolean skip;      // absent/blank and not required → remaining rules ignored
        private boolean stop;      // required failed → remaining rules ignored
        private boolean numeric;   // numeric/integer seen → min/max compare numerically

        private Field(String name) {
            this.name = name;
            this.label = name.replace('_', ' ');
            this.raw = value(name);
            this.skip = raw == null || raw.isBlank();
        }

        private boolean active() {
            return !skip && !stop;
        }

        /** Field must be present and non-blank. */
        public Field required() {
            if (raw == null || raw.isBlank()) {
                add(name, "required", "The " + label + " field is required.");
                stop = true;
            }
            return this;
        }

        /** Required only when {@code condition} holds (required_if / required_unless). */
        public Field requiredIf(boolean condition) {
            return condition ? required() : this;
        }

        /** Absent field is not validated at all. */
        public Field sometimes() {
            if (!data.containsKey(name)) {
                stop = true;
            }
            return this;
        }

        /** Present-but-empty is allowed (rest of the chain is skipped for empty values). */
        public Field nullable() {
            return this;
        }

        public Field string() {
            if (active() && data.get(name) != null && !(data.get(name) instanceof CharSequence)
                    && !(data.get(name) instanceof Number)) {
                add(name, "string", "The " + label + " field must be a string.");
            }
            return this;
        }

        public Field numeric() {
            numeric = true;
            if (active() && !isNumber(raw)) {
                add(name, "numeric", "The " + label + " field must be a number.");
                stop = true;
            }
            return this;
        }

        public Field integer() {
            numeric = true;
            if (active() && !raw.trim().matches("-?\\d+")) {
                add(name, "integer", "The " + label + " field must be an integer.");
                stop = true;
            }
            return this;
        }

        public Field digits(int n) {
            if (active() && !raw.matches("\\d{" + n + "}")) {
                add(name, "digits", "The " + label + " field must be " + n + " digits.");
            }
            return this;
        }

        public Field digitsBetween(int min, int max) {
            if (active() && !raw.matches("\\d{" + min + "," + max + "}")) {
                add(name, "digits_between", "The " + label + " field must be between " + min
                        + " and " + max + " digits.");
            }
            return this;
        }

        public Field min(double n) {
            if (active() && !stop) {
                if (numeric && isNumber(raw)) {
                    if (new BigDecimal(raw.trim()).doubleValue() < n) {
                        add(name, "min", "The " + label + " field must be at least " + fmt(n) + ".");
                    }
                } else if (raw.length() < n) {
                    add(name, "min", "The " + label + " field must be at least " + fmt(n) + " characters.");
                }
            }
            return this;
        }

        public Field max(double n) {
            if (active() && !stop) {
                if (numeric && isNumber(raw)) {
                    if (new BigDecimal(raw.trim()).doubleValue() > n) {
                        add(name, "max", "The " + label + " field must not be greater than " + fmt(n) + ".");
                    }
                } else if (raw.length() > n) {
                    add(name, "max", "The " + label + " field must not be greater than " + fmt(n) + " characters.");
                }
            }
            return this;
        }

        public Field between(double min, double max) {
            if (active() && !stop && isNumber(raw)) {
                double v = new BigDecimal(raw.trim()).doubleValue();
                if (v < min || v > max) {
                    add(name, "between", "The " + label + " field must be between " + fmt(min)
                            + " and " + fmt(max) + ".");
                }
            }
            return this;
        }

        public Field in(String... allowed) {
            if (active() && !Arrays.asList(allowed).contains(raw)) {
                add(name, "in", "The selected " + label + " is invalid.");
            }
            return this;
        }

        public Field regex(String pattern) {
            if (active() && !Pattern.compile(pattern).matcher(raw).find()) {
                add(name, "regex", "The " + label + " field format is invalid.");
            }
            return this;
        }

        public Field email() {
            if (active() && !raw.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                add(name, "email", "The " + label + " field must be a valid email address.");
            }
            return this;
        }

        /** unique:table,column — {@code taken} returns true when the value already exists. */
        public Field unique(Predicate<String> taken) {
            if (active() && taken.test(raw)) {
                add(name, "unique", "The " + label + " has already been taken.");
            }
            return this;
        }

        /** exists:table,column — {@code exists} returns true when the value is found. */
        public Field exists(Predicate<String> exists) {
            if (active() && !exists.test(raw)) {
                add(name, "exists", "The selected " + label + " is invalid.");
            }
            return this;
        }

        /** Arbitrary extra rule. */
        public Field rule(String ruleName, Predicate<String> ok, String message) {
            if (active() && !ok.test(raw)) {
                add(name, ruleName, message);
            }
            return this;
        }
    }

    private static boolean isNumber(String s) {
        if (s == null) return false;
        try {
            new BigDecimal(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String fmt(double n) {
        return n == Math.floor(n) ? String.valueOf((long) n) : String.valueOf(n);
    }
}
