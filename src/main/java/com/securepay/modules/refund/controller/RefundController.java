package com.securepay.modules.refund.controller;

import com.securepay.common.exception.ApiException;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Validation;
import com.securepay.modules.refund.service.RefundService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Port of {@code RefundModule/Controllers/RefundController}. */
@RestController
public class RefundController extends BaseController {

    private final ResponseFactory response;
    private final RefundService refundService;

    public RefundController(ResponseFactory response, RefundService refundService) {
        this.response = response;
        this.refundService = refundService;
    }

    private static boolean isAdmin(CurrentUser cu) {
        return "adm".equals(cu.user().getRole()) || "sadm".equals(cu.user().getRole());
    }

    private Validation txnRules(boolean withOperator) {
        Validation v = Validation.of(payload());
        v.field("txn_id").required().string();
        if (withOperator) {
            v.field("operator_id").required().string();
        }
        return v;
    }

    private ResponseEntity<Map<String, Object>> invalid(Validation v) {
        return response.error("Validation Failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // POST refund/refundTxn
    // The source has NO role check here (any logged-in user could refund any txn) and also lets the cron IP
    // 103.49.124.232 call it without a token. Deliberate hardening: a valid admin login is required.
    @PostMapping("refund/refundTxn")
    public ResponseEntity<Map<String, Object>> refundTxn(@AuthenticationPrincipal CurrentUser cu) {
        if (!isAdmin(cu)) {
            return response.error("Unauthorised Action", "", HttpStatus.FORBIDDEN);
        }
        Validation v = txnRules(false);
        if (v.fails()) {
            return invalid(v);
        }
        try {
            return response.success("Refund processed successfully",
                    refundService.refundTxn(payload().get("txn_id").toString()));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            return response.error("Refund Failed", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST refund/successTxn
    @PostMapping("refund/successTxn")
    public ResponseEntity<Map<String, Object>> successTxn(@AuthenticationPrincipal CurrentUser cu) {
        Validation v = txnRules(true);
        if (v.fails()) {
            return invalid(v);
        }
        if (!isAdmin(cu)) {
            return response.error("Unauthorized", List.of(), HttpStatus.UNAUTHORIZED);
        }
        refundService.markStatus(payload().get("txn_id").toString(), payload().get("operator_id").toString(),
                "SUCCESS", List.of("REFUNDPENDING", "PENDING"));
        return response.success("Transaction marked as SUCCESS", "", HttpStatus.CREATED);
    }

    // POST refund/failedTxn
    @PostMapping("refund/failedTxn")
    public ResponseEntity<Map<String, Object>> failedTxn(@AuthenticationPrincipal CurrentUser cu) {
        Validation v = txnRules(true);
        if (v.fails()) {
            return invalid(v);
        }
        if (!isAdmin(cu)) {
            return response.error("Unauthorized", List.of(), HttpStatus.UNAUTHORIZED);
        }
        refundService.markStatus(payload().get("txn_id").toString(), payload().get("operator_id").toString(),
                "FAILED", List.of("PENDING"));
        return response.success("Transaction marked as FAILED", "", HttpStatus.CREATED);
    }

    // POST refund/successAepsTxn
    @PostMapping("refund/successAepsTxn")
    public ResponseEntity<Map<String, Object>> successAepsTxn(@AuthenticationPrincipal CurrentUser cu) {
        if (!isAdmin(cu)) {
            return response.error("Unauthorized", List.of(), HttpStatus.UNAUTHORIZED);
        }
        Validation v = txnRules(false);
        if (v.fails()) {
            return invalid(v);
        }
        try {
            return response.success("Refund processed successfully",
                    refundService.successAepsTxn(payload().get("txn_id").toString()));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            return response.error("Refund Failed", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
