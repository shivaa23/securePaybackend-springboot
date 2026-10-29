package com.securepay.modules.role.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import com.securepay.modules.role.entity.Role;
import com.securepay.modules.role.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * RoleController — real CRUD over {@link Role} (roles), ported from
 * app/Modules/Role (ResponseHelper success/error envelopes, doc 08).
 */
@RestController
public class RoleController extends BaseController {

    private final ResponseFactory response;
    private final RoleService service;
    private final ObjectMapper objectMapper;

    public RoleController(ResponseFactory response, RoleService service, ObjectMapper objectMapper) {
        this.response = response;
        this.service = service;
        this.objectMapper = objectMapper;
    }

    // GET role/getRoles
    @GetMapping("role/getRoles")
    public ResponseEntity<Map<String, Object>> getRoles() {
        return response.success("Role fetched", service.findAll());
    }

    // POST role/createRole
    @PostMapping("role/createRole")
    public ResponseEntity<Map<String, Object>> createRole() {
        Role entity = objectMapper.convertValue(payload(), Role.class);
        entity.setId(null);
        return response.success("Role created", service.save(entity));
    }

    // POST role/updateRole
    @PostMapping("role/updateRole")
    public ResponseEntity<Map<String, Object>> updateRole() {
        Object idObj = payload().get("id");
        if (idObj == null) {
            return response.error("id is required", "");
        }
        Optional<Role> existing = service.findById(Long.valueOf(idObj.toString()));
        if (existing.isEmpty()) {
            return response.error("Role not found", "");
        }
        Role merged = objectMapper.convertValue(payload(), Role.class);
        merged.setId(existing.get().getId());
        return response.success("Role updated", service.save(merged));
    }

    // POST role/mailCheck
    @PostMapping("role/mailCheck")
    public ResponseEntity<Map<String, Object>> mailCheck() {
        return response.success("Success", payload());
    }
}
