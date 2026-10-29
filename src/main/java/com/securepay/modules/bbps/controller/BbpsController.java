package com.securepay.modules.bbps.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * BbpsController — ports the bbps module endpoints at their exact paths (doc 05).
 *
 * <p>Each endpoint returns the success envelope via {@link ResponseFactory}. Business logic
 * is ported from app/Modules/Bbps in the legacy source; method bodies are
 * scaffolded to preserve the HTTP contract (path/method/envelope) and are filled in per
 * the phased plan (doc 09).</p>
 */
@RestController
public class BbpsController extends BaseController {

    private final ResponseFactory response;

    public BbpsController(ResponseFactory response) {
        this.response = response;
    }

    // POST bbps/getBbpsTxns
    @PostMapping("bbps/getBbpsTxns")
    public ResponseEntity<Map<String, Object>> bbpsGetBbpsTxns() {
        return response.success("Success", payload());
    }

    // POST bbps/getCategories
    @PostMapping("bbps/getCategories")
    public ResponseEntity<Map<String, Object>> bbpsGetCategories() {
        return response.success("Success", payload());
    }

    // POST bbps/getBillers
    @PostMapping("bbps/getBillers")
    public ResponseEntity<Map<String, Object>> bbpsGetBillers() {
        return response.success("Success", payload());
    }

    // POST bbps/getBillerDetails
    @PostMapping("bbps/getBillerDetails")
    public ResponseEntity<Map<String, Object>> bbpsGetBillerDetails() {
        return response.success("Success", payload());
    }

    // POST bbps/fetchBill
    @PostMapping("bbps/fetchBill")
    public ResponseEntity<Map<String, Object>> bbpsFetchBill() {
        return response.success("Success", payload());
    }

    // POST bbps/payBill
    @PostMapping("bbps/payBill")
    public ResponseEntity<Map<String, Object>> bbpsPayBill() {
        return response.success("Success", payload());
    }

    // POST bbps/getCategoriesBillPayment
    @PostMapping("bbps/getCategoriesBillPayment")
    public ResponseEntity<Map<String, Object>> bbpsGetCategoriesBillPayment() {
        return response.success("Success", payload());
    }

    // POST bbps/billPayment
    @PostMapping("bbps/billPayment")
    public ResponseEntity<Map<String, Object>> bbpsBillPayment() {
        return response.success("Success", payload());
    }

    // POST bbps/partPayment
    @PostMapping("bbps/partPayment")
    public ResponseEntity<Map<String, Object>> bbpsPartPayment() {
        return response.success("Success", payload());
    }
}
