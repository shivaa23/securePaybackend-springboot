package com.securepay.modules.kyc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import com.securepay.modules.kyc.entity.Kyc;
import com.securepay.modules.kyc.service.KycService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * KycController — real CRUD over {@link Kyc} (kycs), ported from
 * app/Modules/Kyc (ResponseHelper success/error envelopes, doc 08).
 */
@RestController
public class KycController extends BaseController {

    private final ResponseFactory response;
    private final KycService service;
    private final ObjectMapper objectMapper;

    public KycController(ResponseFactory response, KycService service, ObjectMapper objectMapper) {
        this.response = response;
        this.service = service;
        this.objectMapper = objectMapper;
    }

    // GET kyc/getKycs
    @GetMapping("kyc/getKycs")
    public ResponseEntity<Map<String, Object>> getKycs() {
        return response.success("Kyc fetched", service.findAll());
    }

    // POST kyc/createKyc
    @PostMapping("kyc/createKyc")
    public ResponseEntity<Map<String, Object>> createKyc() {
        Kyc entity = objectMapper.convertValue(payload(), Kyc.class);
        entity.setId(null);
        return response.success("Kyc created", service.save(entity));
    }

    // POST kyc/updateKyc
    @PostMapping("kyc/updateKyc")
    public ResponseEntity<Map<String, Object>> updateKyc() {
        Object idObj = payload().get("id");
        if (idObj == null) {
            return response.error("id is required", "");
        }
        Optional<Kyc> existing = service.findById(Long.valueOf(idObj.toString()));
        if (existing.isEmpty()) {
            return response.error("Kyc not found", "");
        }
        Kyc merged = objectMapper.convertValue(payload(), Kyc.class);
        merged.setId(existing.get().getId());
        return response.success("Kyc updated", service.save(merged));
    }
}
