package com.securepay.modules.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import com.securepay.modules.template.entity.Template;
import com.securepay.modules.template.service.TemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * TemplateController — real CRUD over {@link Template} (templates), ported from
 * app/Modules/Template (ResponseHelper success/error envelopes, doc 08).
 */
@RestController
public class TemplateController extends BaseController {

    private final ResponseFactory response;
    private final TemplateService service;
    private final ObjectMapper objectMapper;

    public TemplateController(ResponseFactory response, TemplateService service, ObjectMapper objectMapper) {
        this.response = response;
        this.service = service;
        this.objectMapper = objectMapper;
    }

    // POST template/getTemplates
    @PostMapping("template/getTemplates")
    public ResponseEntity<Map<String, Object>> getTemplates() {
        return response.success("Template fetched", service.findAll());
    }

    // POST template/createTemplate
    @PostMapping("template/createTemplate")
    public ResponseEntity<Map<String, Object>> createTemplate() {
        Template entity = objectMapper.convertValue(payload(), Template.class);
        entity.setId(null);
        return response.success("Template created", service.save(entity));
    }

    // POST template/updateTemplate
    @PostMapping("template/updateTemplate")
    public ResponseEntity<Map<String, Object>> updateTemplate() {
        Object idObj = payload().get("id");
        if (idObj == null) {
            return response.error("id is required", "");
        }
        Optional<Template> existing = service.findById(Long.valueOf(idObj.toString()));
        if (existing.isEmpty()) {
            return response.error("Template not found", "");
        }
        Template merged = objectMapper.convertValue(payload(), Template.class);
        merged.setId(existing.get().getId());
        return response.success("Template updated", service.save(merged));
    }

    // POST template/deleteTemplate
    @PostMapping("template/deleteTemplate")
    public ResponseEntity<Map<String, Object>> deleteTemplate() {
        Object idObj = payload().get("id");
        if (idObj == null) {
            return response.error("id is required", "");
        }
        boolean ok = service.deleteById(Long.valueOf(idObj.toString()));
        return ok ? response.success("Template deleted", "")
                : response.error("Template not found", "");
    }
}
