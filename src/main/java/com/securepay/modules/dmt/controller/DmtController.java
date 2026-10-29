package com.securepay.modules.dmt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.ratelimit.RateLimit;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.modules.dmt.dto.DmtTxnRequest;
import com.securepay.modules.dmt.service.DmtTransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * DmtController — ports the dmt module endpoints at their exact paths (doc 05).
 *
 * <p>The transaction endpoint {@code dmt1/dmtTxn} is fully wired to {@link DmtTransactionService}
 * (commission → split → W1 debit → ImpsGuru provider → refund-on-fail). The remaining endpoints
 * return the envelope and are filled in per the phased plan (doc 09).</p>
 */
@RestController
public class DmtController extends BaseController {

    private final ResponseFactory response;
    private final DmtTransactionService dmtTransactionService;
    private final ObjectMapper objectMapper;

    public DmtController(ResponseFactory response, DmtTransactionService dmtTransactionService,
                         ObjectMapper objectMapper) {
        this.response = response;
        this.dmtTransactionService = dmtTransactionService;
        this.objectMapper = objectMapper;
    }

    private String clientIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // POST dmt/getDmtTxns
    @PostMapping("dmt/getDmtTxns")
    public ResponseEntity<Map<String, Object>> dmtGetDmtTxns() {
        return response.success("Success", payload());
    }

    // POST dmt/getaccVerifyTxns
    @PostMapping("dmt/getaccVerifyTxns")
    public ResponseEntity<Map<String, Object>> dmtGetaccVerifyTxns() {
        return response.success("Success", payload());
    }

    // POST dmt1/getBanks
    @PostMapping("dmt1/getBanks")
    public ResponseEntity<Map<String, Object>> dmt1GetBanks() {
        return response.success("Success", payload());
    }

    // POST dmt1/getBeneficiarySchema
    @PostMapping("dmt1/getBeneficiarySchema")
    public ResponseEntity<Map<String, Object>> dmt1GetBeneficiarySchema() {
        return response.success("Success", payload());
    }

    // POST dmt1/remitterStatus
    @PostMapping("dmt1/remitterStatus")
    public ResponseEntity<Map<String, Object>> dmt1RemitterStatus() {
        return response.success("Success", payload());
    }

    // POST dmt1/registerBeneficiary
    @PostMapping("dmt1/registerBeneficiary")
    public ResponseEntity<Map<String, Object>> dmt1RegisterBeneficiary() {
        return response.success("Success", payload());
    }

    // POST dmt1/VerifyRegisterBeneficiary
    @PostMapping("dmt1/VerifyRegisterBeneficiary")
    public ResponseEntity<Map<String, Object>> dmt1VerifyRegisterBeneficiary() {
        return response.success("Success", payload());
    }

    // POST dmt1/removeBeneficiary
    @PostMapping("dmt1/removeBeneficiary")
    public ResponseEntity<Map<String, Object>> dmt1RemoveBeneficiary() {
        return response.success("Success", payload());
    }

    // POST dmt1/VerifyRemoveBeneficiary
    @PostMapping("dmt1/VerifyRemoveBeneficiary")
    public ResponseEntity<Map<String, Object>> dmt1VerifyRemoveBeneficiary() {
        return response.success("Success", payload());
    }

    // POST dmt1/registerRemitter
    @PostMapping("dmt1/registerRemitter")
    public ResponseEntity<Map<String, Object>> dmt1RegisterRemitter() {
        return response.success("Success", payload());
    }

    // POST dmt1/validateRemitter
    @PostMapping("dmt1/validateRemitter")
    public ResponseEntity<Map<String, Object>> dmt1ValidateRemitter() {
        return response.success("Success", payload());
    }

    // POST dmt1/kycRemitter
    @PostMapping("dmt1/kycRemitter")
    public ResponseEntity<Map<String, Object>> dmt1KycRemitter() {
        return response.success("Success", payload());
    }

    // POST dmt1/dmtTxn — full transaction orchestration (doc 10 §10.8)
    @PostMapping("dmt1/dmtTxn")
    @RateLimit(requests = 100, minutes = 1)
    public ResponseEntity<Map<String, Object>> dmt1DmtTxn(
            @AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest request) {
        DmtTxnRequest dto = objectMapper.convertValue(payload(), DmtTxnRequest.class);
        String serviceName = payloadString("operator") != null ? payloadString("operator") : "DMT1";
        Map<String, Object> result = dmtTransactionService.dmtTxn(
                currentUser.user(), serviceName, dto, request.getHeader("User-Agent"), clientIp(request));
        return response.success("Success", result);
    }

    // POST dmt1/transactionOtpDmt1
    @PostMapping("dmt1/transactionOtpDmt1")
    public ResponseEntity<Map<String, Object>> dmt1TransactionOtpDmt1() {
        return response.success("Success", payload());
    }

    // POST dmt1/accountVerification
    @PostMapping("dmt1/accountVerification")
    public ResponseEntity<Map<String, Object>> dmt1AccountVerification() {
        return response.success("Success", payload());
    }

    // POST dmt1/getRegisterRemitterSchema
    @PostMapping("dmt1/getRegisterRemitterSchema")
    public ResponseEntity<Map<String, Object>> dmt1GetRegisterRemitterSchema() {
        return response.success("Success", payload());
    }

    // POST dmt2/getDmtTxns
    @PostMapping("dmt2/getDmtTxns")
    public ResponseEntity<Map<String, Object>> dmt2GetDmtTxns() {
        return response.success("Success", payload());
    }

    // POST dmt2/getBanksDmt2
    @PostMapping("dmt2/getBanksDmt2")
    public ResponseEntity<Map<String, Object>> dmt2GetBanksDmt2() {
        return response.success("Success", payload());
    }

    // POST dmt2/getBeneficiarySchemaDmt2
    @PostMapping("dmt2/getBeneficiarySchemaDmt2")
    public ResponseEntity<Map<String, Object>> dmt2GetBeneficiarySchemaDmt2() {
        return response.success("Success", payload());
    }

    // POST dmt2/remitterStatusDmt2
    @PostMapping("dmt2/remitterStatusDmt2")
    public ResponseEntity<Map<String, Object>> dmt2RemitterStatusDmt2() {
        return response.success("Success", payload());
    }

    // POST dmt2/registerBeneficiaryDmt2
    @PostMapping("dmt2/registerBeneficiaryDmt2")
    public ResponseEntity<Map<String, Object>> dmt2RegisterBeneficiaryDmt2() {
        return response.success("Success", payload());
    }

    // POST dmt2/VerifyRegisterBeneficiary
    @PostMapping("dmt2/VerifyRegisterBeneficiary")
    public ResponseEntity<Map<String, Object>> dmt2VerifyRegisterBeneficiary() {
        return response.success("Success", payload());
    }

    // POST dmt2/removeBeneficiaryDmt2
    @PostMapping("dmt2/removeBeneficiaryDmt2")
    public ResponseEntity<Map<String, Object>> dmt2RemoveBeneficiaryDmt2() {
        return response.success("Success", payload());
    }

    // POST dmt2/VerifyRemoveBeneficiary
    @PostMapping("dmt2/VerifyRemoveBeneficiary")
    public ResponseEntity<Map<String, Object>> dmt2VerifyRemoveBeneficiary() {
        return response.success("Success", payload());
    }

    // POST dmt2/registerRemitter
    @PostMapping("dmt2/registerRemitter")
    public ResponseEntity<Map<String, Object>> dmt2RegisterRemitter() {
        return response.success("Success", payload());
    }

    // POST dmt2/kycRemitter
    @PostMapping("dmt2/kycRemitter")
    public ResponseEntity<Map<String, Object>> dmt2KycRemitter() {
        return response.success("Success", payload());
    }

    // POST dmt2/dmt2Txn
    @PostMapping("dmt2/dmt2Txn")
    public ResponseEntity<Map<String, Object>> dmt2Dmt2Txn() {
        return response.success("Success", payload());
    }

    // POST dmt2/transactionOtpDmt2
    @PostMapping("dmt2/transactionOtpDmt2")
    public ResponseEntity<Map<String, Object>> dmt2TransactionOtpDmt2() {
        return response.success("Success", payload());
    }

    // POST dmt2/accountVerification
    @PostMapping("dmt2/accountVerification")
    public ResponseEntity<Map<String, Object>> dmt2AccountVerification() {
        return response.success("Success", payload());
    }

    // GET dmt2/queue-test
    @GetMapping("dmt2/queue-test")
    public ResponseEntity<Map<String, Object>> getDmt2QueueTest() {
        return response.success("Success", payload());
    }
}
