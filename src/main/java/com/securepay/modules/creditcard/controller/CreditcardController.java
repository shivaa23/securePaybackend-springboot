package com.securepay.modules.creditcard.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * CreditcardController — ports the creditcard module endpoints at their exact paths (doc 05).
 *
 * <p>Each endpoint returns the success envelope via {@link ResponseFactory}. Business logic
 * is ported from app/Modules/Creditcard in the legacy source; method bodies are
 * scaffolded to preserve the HTTP contract (path/method/envelope) and are filled in per
 * the phased plan (doc 09).</p>
 */
@RestController
public class CreditcardController extends BaseController {

    private final ResponseFactory response;

    public CreditcardController(ResponseFactory response) {
        this.response = response;
    }

    // POST creditcard/payCreditCard
    @PostMapping("creditcard/payCreditCard")
    public ResponseEntity<Map<String, Object>> creditcardPayCreditCard() {
        return response.success("Success", payload());
    }

    // POST creditcard/getCreditCard
    @PostMapping("creditcard/getCreditCard")
    public ResponseEntity<Map<String, Object>> creditcardGetCreditCard() {
        return response.success("Success", payload());
    }
}
