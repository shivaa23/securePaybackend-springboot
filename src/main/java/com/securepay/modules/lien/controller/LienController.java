package com.securepay.modules.lien.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * LienController — ports the lien module endpoints at their exact paths (doc 05).
 *
 * <p>Each endpoint returns the success envelope via {@link ResponseFactory}. Business logic
 * is ported from app/Modules/Lien in the legacy source; method bodies are
 * scaffolded to preserve the HTTP contract (path/method/envelope) and are filled in per
 * the phased plan (doc 09).</p>
 */
@RestController
public class LienController extends BaseController {

    private final ResponseFactory response;

    public LienController(ResponseFactory response) {
        this.response = response;
    }

    // POST lien/addLien
    @PostMapping("lien/addLien")
    public ResponseEntity<Map<String, Object>> lienAddLien() {
        return response.success("Success", payload());
    }

    // POST lien/removeLien
    @PostMapping("lien/removeLien")
    public ResponseEntity<Map<String, Object>> lienRemoveLien() {
        return response.success("Success", payload());
    }

    // POST lien/getLien
    @PostMapping("lien/getLien")
    public ResponseEntity<Map<String, Object>> lienGetLien() {
        return response.success("Success", payload());
    }
}
