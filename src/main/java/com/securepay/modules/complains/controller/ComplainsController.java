package com.securepay.modules.complains.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import com.securepay.modules.complains.entity.Complains;
import com.securepay.modules.complains.service.ComplainsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * ComplainsController — real CRUD over {@link Complains} (complains), ported from
 * app/Modules/Complains (ResponseHelper success/error envelopes, doc 08).
 */
@RestController
public class ComplainsController extends BaseController {

    private final ResponseFactory response;
    private final ComplainsService service;
    private final ObjectMapper objectMapper;

    public ComplainsController(ResponseFactory response, ComplainsService service, ObjectMapper objectMapper) {
        this.response = response;
        this.service = service;
        this.objectMapper = objectMapper;
    }

    // POST complains/getComplains
    @PostMapping("complains/getComplains")
    public ResponseEntity<Map<String, Object>> getComplains() {
        return response.success("Complains fetched", service.findAll());
    }

    // POST complains/createComplains
    @PostMapping("complains/createComplains")
    public ResponseEntity<Map<String, Object>> createComplains() {
        Complains entity = objectMapper.convertValue(payload(), Complains.class);
        entity.setId(null);
        return response.success("Complains created", service.save(entity));
    }

    // POST complains/updateComplains
    @PostMapping("complains/updateComplains")
    public ResponseEntity<Map<String, Object>> updateComplains() {
        Object idObj = payload().get("id");
        if (idObj == null) {
            return response.error("id is required", "");
        }
        Optional<Complains> existing = service.findById(Long.valueOf(idObj.toString()));
        if (existing.isEmpty()) {
            return response.error("Complains not found", "");
        }
        Complains merged = objectMapper.convertValue(payload(), Complains.class);
        merged.setId(existing.get().getId());
        return response.success("Complains updated", service.save(merged));
    }
}
