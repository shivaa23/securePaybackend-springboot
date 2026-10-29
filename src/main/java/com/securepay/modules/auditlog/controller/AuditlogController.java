package com.securepay.modules.auditlog.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AuditlogController — ports the auditlog module endpoints at their exact paths (doc 05).
 *
 * <p>Each endpoint returns the success envelope via {@link ResponseFactory}. Business logic
 * is ported from app/Modules/Auditlog in the legacy source; method bodies are
 * scaffolded to preserve the HTTP contract (path/method/envelope) and are filled in per
 * the phased plan (doc 09).</p>
 */
@RestController
public class AuditlogController extends BaseController {

    private final ResponseFactory response;

    public AuditlogController(ResponseFactory response) {
        this.response = response;
    }

    // POST auditlog/getAuditLogs
    @PostMapping("auditlog/getAuditLogs")
    public ResponseEntity<Map<String, Object>> auditlogGetAuditLogs() {
        return response.success("Success", payload());
    }

    // POST auditlog/getAuditLog
    @PostMapping("auditlog/getAuditLog")
    public ResponseEntity<Map<String, Object>> auditlogGetAuditLog() {
        return response.success("Success", payload());
    }

    // POST auditlog/deleteAuditLog
    @PostMapping("auditlog/deleteAuditLog")
    public ResponseEntity<Map<String, Object>> auditlogDeleteAuditLog() {
        return response.success("Success", payload());
    }
}
