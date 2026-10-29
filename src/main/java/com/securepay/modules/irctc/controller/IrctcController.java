package com.securepay.modules.irctc.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * IrctcController — ports the irctc module endpoints at their exact paths (doc 05).
 *
 * <p>Each endpoint returns the success envelope via {@link ResponseFactory}. Business logic
 * is ported from app/Modules/Irctc in the legacy source; method bodies are
 * scaffolded to preserve the HTTP contract (path/method/envelope) and are filled in per
 * the phased plan (doc 09).</p>
 */
@RestController
public class IrctcController extends BaseController {

    private final ResponseFactory response;

    public IrctcController(ResponseFactory response) {
        this.response = response;
    }

    // POST irctc/getIrctcTxns
    @PostMapping("irctc/getIrctcTxns")
    public ResponseEntity<Map<String, Object>> irctcGetIrctcTxns() {
        return response.success("Success", payload());
    }
}
