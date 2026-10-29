package com.securepay.common.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.crypto.PayloadCrypto;
import com.securepay.config.SecurePayProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single source of truth for response envelope parity (doc 08 §8.3).
 *
 * <p>Produces the four legacy envelope shapes:</p>
 * <ul>
 *   <li>{@link #success} / {@link #error} — {@code ResponseHelper} family A (encrypted envelopes).</li>
 *   <li>{@link #jsonResponse} / {@link #prepaidResponse} — {@code ApiResponser} family B.</li>
 * </ul>
 *
 * <p>Field order is controlled via {@link LinkedHashMap} to match PHP insertion order.</p>
 */
@Component
public class ResponseFactory {

    private final PayloadCrypto payloadCrypto;
    private final ObjectMapper objectMapper;
    private final ZoneId zoneId;
    private final String environmentLabel;

    // Legacy date('Y-m-d h:i:s'): 12-hour, zero-padded, NO am/pm marker (doc 08 §8.2).
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");

    public ResponseFactory(PayloadCrypto payloadCrypto, ObjectMapper objectMapper,
                           SecurePayProperties properties) {
        this.payloadCrypto = payloadCrypto;
        this.objectMapper = objectMapper;
        SecurePayProperties.App app = properties.app();
        this.zoneId = ZoneId.of(app != null && app.timezone() != null ? app.timezone() : "Asia/Kolkata");
        this.environmentLabel = app != null && app.environmentLabel() != null ? app.environmentLabel() : "Live";
    }

    // ------------------------------------------------------------------
    // Family A — ResponseHelper (encrypted envelopes)
    // ------------------------------------------------------------------

    /** Port of {@code ResponseHelper::success($message, $data, $status=200)}. */
    public ResponseEntity<Map<String, Object>> success(String message, Object data) {
        return success(message, data, HttpStatus.OK);
    }

    public ResponseEntity<Map<String, Object>> success(String message, Object data, HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", true);
        body.put("message", message);
        body.put("data", data);
        body.put("encrypted_data", encrypt(data));
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Same as {@link #error} but with a non-string message: the legacy code sometimes passes the
     * validator's MessageBag as the message ({@code ResponseHelper::error($validator->errors(), [], 400)}).
     */
    public ResponseEntity<Map<String, Object>> errorWithObjectMessage(Object message, Object errors,
                                                                      HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", false);
        body.put("message", message);
        body.put("errors", errors);
        body.put("encrypted_errors", encrypt(errors));
        return ResponseEntity.status(status).body(body);
    }

    /** Port of {@code ResponseHelper::error($message, $errors, $status=400)}. */
    public ResponseEntity<Map<String, Object>> error(String message, Object errors) {
        return error(message, errors, HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<Map<String, Object>> error(String message, Object errors, HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", false);
        body.put("message", message);
        body.put("errors", errors);
        body.put("encrypted_errors", encrypt(errors));
        return ResponseEntity.status(status).body(body);
    }

    // ------------------------------------------------------------------
    // Family B — ApiResponser trait
    // ------------------------------------------------------------------

    /** Port of {@code ApiResponser::jsonResponse($statuscode, $status, $message, $data, $code=200)}. */
    public ResponseEntity<Map<String, Object>> jsonResponse(Object status, String message, Object data) {
        return jsonResponse(status, message, data, HttpStatus.OK);
    }

    public ResponseEntity<Map<String, Object>> jsonResponse(Object status, String message, Object data,
                                                            HttpStatus code) {
        Map<String, Object> body = new LinkedHashMap<>();
        // NOTE: statuscode is commented out in the legacy jsonResponse → intentionally omitted.
        body.put("status", status);
        body.put("message", message);
        body.put("data", data);
        body.put("timestamp", now());
        body.put("environment", environmentLabel);
        return ResponseEntity.status(code).body(body);
    }

    /**
     * Port of {@code ApiResponser::prepaidResponse($statuscode, $status, $message, $data, $info, $code)}.
     * Here {@code statuscode} IS included, plus an extra {@code info} field.
     */
    public ResponseEntity<Map<String, Object>> prepaidResponse(Object statuscode, Object status,
                                                               String message, Object data, Object info,
                                                               HttpStatus code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("statuscode", statuscode);
        body.put("status", status);
        body.put("message", message);
        body.put("data", data);
        body.put("info", info);
        body.put("timestamp", now());
        body.put("environment", environmentLabel);
        return ResponseEntity.status(code).body(body);
    }

    // ------------------------------------------------------------------

    private String now() {
        return ZonedDateTime.now(zoneId).format(TIMESTAMP_FMT);
    }

    private String encrypt(Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return payloadCrypto.encrypt(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize response data for encryption", e);
        }
    }
}
