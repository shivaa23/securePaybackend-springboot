package com.securepay.modules.cms.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * CmsController — ports the cms module endpoints at their exact paths (doc 05).
 *
 * <p>Each endpoint returns the success envelope via {@link ResponseFactory}. Business logic
 * is ported from app/Modules/Cms in the legacy source; method bodies are
 * scaffolded to preserve the HTTP contract (path/method/envelope) and are filled in per
 * the phased plan (doc 09).</p>
 */
@RestController
public class CmsController extends BaseController {

    private final ResponseFactory response;

    public CmsController(ResponseFactory response) {
        this.response = response;
    }

    // POST cms/createOrderCms
    @PostMapping("cms/createOrderCms")
    public ResponseEntity<Map<String, Object>> cmsCreateOrderCms() {
        return response.success("Success", payload());
    }

    // POST cms/createOrderMatm
    @PostMapping("cms/createOrderMatm")
    public ResponseEntity<Map<String, Object>> cmsCreateOrderMatm() {
        return response.success("Success", payload());
    }
}
