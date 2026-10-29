package com.securepay.modules.bank.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * BankController — ports the bank module endpoints at their exact paths (doc 05).
 *
 * <p>Each endpoint returns the success envelope via {@link ResponseFactory}. Business logic
 * is ported from app/Modules/Bank in the legacy source; method bodies are
 * scaffolded to preserve the HTTP contract (path/method/envelope) and are filled in per
 * the phased plan (doc 09).</p>
 */
@RestController
public class BankController extends BaseController {

    private final ResponseFactory response;

    public BankController(ResponseFactory response) {
        this.response = response;
    }

    // POST bank/getBanks
    @PostMapping("bank/getBanks")
    public ResponseEntity<Map<String, Object>> bankGetBanks() {
        return response.success("Success", payload());
    }

    // POST bank/getBankSchema
    @PostMapping("bank/getBankSchema")
    public ResponseEntity<Map<String, Object>> bankGetBankSchema() {
        return response.success("Success", payload());
    }

    // POST bank/getBankBalance
    @PostMapping("bank/getBankBalance")
    public ResponseEntity<Map<String, Object>> bankGetBankBalance() {
        return response.success("Success", payload());
    }

    // POST bank/createBank
    @PostMapping("bank/createBank")
    public ResponseEntity<Map<String, Object>> bankCreateBank() {
        return response.success("Success", payload());
    }

    // POST bank/updateBank
    @PostMapping("bank/updateBank")
    public ResponseEntity<Map<String, Object>> bankUpdateBank() {
        return response.success("Success", payload());
    }

    // POST bank/deleteBank
    @PostMapping("bank/deleteBank")
    public ResponseEntity<Map<String, Object>> bankDeleteBank() {
        return response.success("Success", payload());
    }

    // POST bank/deleteLastTxn
    @PostMapping("bank/deleteLastTxn")
    public ResponseEntity<Map<String, Object>> bankDeleteLastTxn() {
        return response.success("Success", payload());
    }

    // POST bank/getUnclaimedEntries
    @PostMapping("bank/getUnclaimedEntries")
    public ResponseEntity<Map<String, Object>> bankGetUnclaimedEntries() {
        return response.success("Success", payload());
    }

    // POST bankStatement/getBankStatement
    @PostMapping("bankStatement/getBankStatement")
    public ResponseEntity<Map<String, Object>> bankStatementGetBankStatement() {
        return response.success("Success", payload());
    }

    // POST bankStatement/getBankStatementSchema
    @PostMapping("bankStatement/getBankStatementSchema")
    public ResponseEntity<Map<String, Object>> bankStatementGetBankStatementSchema() {
        return response.success("Success", payload());
    }

    // POST bankStatement/createBankStatement
    @PostMapping("bankStatement/createBankStatement")
    public ResponseEntity<Map<String, Object>> bankStatementCreateBankStatement() {
        return response.success("Success", payload());
    }

    // POST bankStatement/updateBankStatement
    @PostMapping("bankStatement/updateBankStatement")
    public ResponseEntity<Map<String, Object>> bankStatementUpdateBankStatement() {
        return response.success("Success", payload());
    }

    // POST bankStatement/deleteBankStatement
    @PostMapping("bankStatement/deleteBankStatement")
    public ResponseEntity<Map<String, Object>> bankStatementDeleteBankStatement() {
        return response.success("Success", payload());
    }
}
