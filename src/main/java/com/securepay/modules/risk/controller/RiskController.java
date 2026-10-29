package com.securepay.modules.risk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import com.securepay.modules.risk.entity.BlackListedAcc;
import com.securepay.modules.risk.entity.WhiteListedAcc;
import com.securepay.modules.risk.service.RiskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Risk module — whitelist/blacklist account CRUD (doc 05 §Risk), real persistence over
 * {@code white_listed_accs} / {@code black_listed_accs}.
 */
@RestController
public class RiskController extends BaseController {

    private final ResponseFactory response;
    private final RiskService service;
    private final ObjectMapper objectMapper;

    public RiskController(ResponseFactory response, RiskService service, ObjectMapper objectMapper) {
        this.response = response;
        this.service = service;
        this.objectMapper = objectMapper;
    }

    // ---- Whitelist ----
    @PostMapping("risk/getWhiteListedAccount")
    public ResponseEntity<Map<String, Object>> getWhiteListedAccount() {
        return response.success("Whitelisted accounts", service.allWhite());
    }

    @PostMapping("risk/createWhiteListedAccount")
    public ResponseEntity<Map<String, Object>> createWhiteListedAccount() {
        WhiteListedAcc acc = objectMapper.convertValue(payload(), WhiteListedAcc.class);
        acc.setId(null);
        return response.success("Whitelisted account created", service.saveWhite(acc));
    }

    @PostMapping("risk/updateWhiteListedAccount")
    public ResponseEntity<Map<String, Object>> updateWhiteListedAccount() {
        Object idObj = payload().get("id");
        if (idObj == null) {
            return response.error("id is required", "");
        }
        Optional<WhiteListedAcc> existing = service.findWhite(Long.valueOf(idObj.toString()));
        if (existing.isEmpty()) {
            return response.error("Whitelisted account not found", "");
        }
        WhiteListedAcc merged = objectMapper.convertValue(payload(), WhiteListedAcc.class);
        merged.setId(existing.get().getId());
        return response.success("Whitelisted account updated", service.saveWhite(merged));
    }

    @PostMapping("risk/deleteWhiteListedAccount")
    public ResponseEntity<Map<String, Object>> deleteWhiteListedAccount() {
        Object idObj = payload().get("id");
        if (idObj == null) {
            return response.error("id is required", "");
        }
        boolean ok = service.deleteWhite(Long.valueOf(idObj.toString()));
        return ok ? response.success("Whitelisted account deleted", "")
                : response.error("Whitelisted account not found", "");
    }

    // ---- Blacklist ----
    @PostMapping("risk/getBlackListedAccount")
    public ResponseEntity<Map<String, Object>> getBlackListedAccount() {
        return response.success("Blacklisted accounts", service.allBlack());
    }

    @PostMapping("risk/createBlackListedAccount")
    public ResponseEntity<Map<String, Object>> createBlackListedAccount() {
        BlackListedAcc acc = objectMapper.convertValue(payload(), BlackListedAcc.class);
        acc.setId(null);
        return response.success("Blacklisted account created", service.saveBlack(acc));
    }

    @PostMapping("risk/updateBlackListedAccount")
    public ResponseEntity<Map<String, Object>> updateBlackListedAccount() {
        Object idObj = payload().get("id");
        if (idObj == null) {
            return response.error("id is required", "");
        }
        Optional<BlackListedAcc> existing = service.findBlack(Long.valueOf(idObj.toString()));
        if (existing.isEmpty()) {
            return response.error("Blacklisted account not found", "");
        }
        BlackListedAcc merged = objectMapper.convertValue(payload(), BlackListedAcc.class);
        merged.setId(existing.get().getId());
        return response.success("Blacklisted account updated", service.saveBlack(merged));
    }

    @PostMapping("risk/deleteBlackListedAccount")
    public ResponseEntity<Map<String, Object>> deleteBlackListedAccount() {
        Object idObj = payload().get("id");
        if (idObj == null) {
            return response.error("id is required", "");
        }
        boolean ok = service.deleteBlack(Long.valueOf(idObj.toString()));
        return ok ? response.success("Blacklisted account deleted", "")
                : response.error("Blacklisted account not found", "");
    }
}
