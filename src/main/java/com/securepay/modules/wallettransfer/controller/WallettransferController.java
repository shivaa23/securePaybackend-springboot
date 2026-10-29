package com.securepay.modules.wallettransfer.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * WallettransferController — ports the wallettransfer module endpoints at their exact paths (doc 05).
 *
 * <p>Each endpoint returns the success envelope via {@link ResponseFactory}. Business logic
 * is ported from app/Modules/Wallettransfer in the legacy source; method bodies are
 * scaffolded to preserve the HTTP contract (path/method/envelope) and are filled in per
 * the phased plan (doc 09).</p>
 */
@RestController
public class WallettransferController extends BaseController {

    private final ResponseFactory response;

    public WallettransferController(ResponseFactory response) {
        this.response = response;
    }

    // POST wallet/getW2WTransactions
    @PostMapping("wallet/getW2WTransactions")
    public ResponseEntity<Map<String, Object>> walletGetW2WTransactions() {
        return response.success("Success", payload());
    }

    // POST wallet/getReceiver
    @PostMapping("wallet/getReceiver")
    public ResponseEntity<Map<String, Object>> walletGetReceiver() {
        return response.success("Success", payload());
    }

    // POST wallet/getW2WTransaction
    @PostMapping("wallet/getW2WTransaction")
    public ResponseEntity<Map<String, Object>> walletGetW2WTransaction() {
        return response.success("Success", payload());
    }

    // POST wallet/createW2W
    @PostMapping("wallet/createW2W")
    public ResponseEntity<Map<String, Object>> walletCreateW2W() {
        return response.success("Success", payload());
    }

    // POST wallet/createW2W1
    @PostMapping("wallet/createW2W1")
    public ResponseEntity<Map<String, Object>> walletCreateW2W1() {
        return response.success("Success", payload());
    }

    // POST wallet/createW3W1
    @PostMapping("wallet/createW3W1")
    public ResponseEntity<Map<String, Object>> walletCreateW3W1() {
        return response.success("Success", payload());
    }

    // POST wallet/createDebit
    @PostMapping("wallet/createDebit")
    public ResponseEntity<Map<String, Object>> walletCreateDebit() {
        return response.success("Success", payload());
    }

    // POST wallet/getDebitWallateTxn
    @PostMapping("wallet/getDebitWallateTxn")
    public ResponseEntity<Map<String, Object>> walletGetDebitWallateTxn() {
        return response.success("Success", payload());
    }

    // POST wallet/getCreditDebitReport
    @PostMapping("wallet/getCreditDebitReport")
    public ResponseEntity<Map<String, Object>> walletGetCreditDebitReport() {
        return response.success("Success", payload());
    }
}
