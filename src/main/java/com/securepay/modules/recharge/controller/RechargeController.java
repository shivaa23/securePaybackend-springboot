package com.securepay.modules.recharge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.modules.recharge.dto.RechargeRequest;
import com.securepay.modules.recharge.service.RechargeTransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * RechargeController — ports the recharge module endpoints at their exact paths (doc 05).
 *
 * <p>The transaction endpoint {@code recharge/recharge} is fully wired to
 * {@link RechargeTransactionService} (commission → debit → provider → refund-on-fail). The
 * remaining endpoints return the envelope and are filled in per the phased plan (doc 09).</p>
 */
@RestController
public class RechargeController extends BaseController {

    private final ResponseFactory response;
    private final RechargeTransactionService rechargeTransactionService;
    private final ObjectMapper objectMapper;

    public RechargeController(ResponseFactory response,
                              RechargeTransactionService rechargeTransactionService,
                              ObjectMapper objectMapper) {
        this.response = response;
        this.rechargeTransactionService = rechargeTransactionService;
        this.objectMapper = objectMapper;
    }

    // POST recharge/get-recharge-txns
    @PostMapping("recharge/get-recharge-txns")
    public ResponseEntity<Map<String, Object>> rechargeGetRechargeTxns() {
        return response.success("Success", payload());
    }

    // POST recharge/getRechargeTxns
    @PostMapping("recharge/getRechargeTxns")
    public ResponseEntity<Map<String, Object>> rechargeGetRechargeTxns2() {
        return response.success("Success", payload());
    }

    // POST recharge/recharge  — full transaction orchestration (doc 10 §10.8)
    @PostMapping("recharge/recharge")
    public ResponseEntity<Map<String, Object>> rechargeRecharge(
            @AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest request) {
        RechargeRequest dto = objectMapper.convertValue(payload(), RechargeRequest.class);
        Map<String, Object> result = rechargeTransactionService.recharge(
                currentUser.user(), dto, request.getHeader("User-Agent"), clientIp(request));
        return response.success("Success", result);
    }

    // POST recharge/getPlanByCircle
    @PostMapping("recharge/getPlanByCircle")
    public ResponseEntity<Map<String, Object>> rechargeGetPlanByCircle() {
        return response.success("Success", payload());
    }

    // POST recharge/getPlansByOperator
    @PostMapping("recharge/getPlansByOperator")
    public ResponseEntity<Map<String, Object>> rechargeGetPlansByOperator() {
        return response.success("Success", payload());
    }

    // POST recharge/createRechargePlan
    @PostMapping("recharge/createRechargePlan")
    public ResponseEntity<Map<String, Object>> rechargeCreateRechargePlan() {
        return response.success("Success", payload());
    }

    // POST recharge/getPlansSchema
    @PostMapping("recharge/getPlansSchema")
    public ResponseEntity<Map<String, Object>> rechargeGetPlansSchema() {
        return response.success("Success", payload());
    }

    // POST recharge/getRechargePlans
    @PostMapping("recharge/getRechargePlans")
    public ResponseEntity<Map<String, Object>> rechargeGetRechargePlans() {
        return response.success("Success", payload());
    }

    // POST recharge/fetchPlan
    @PostMapping("recharge/fetchPlan")
    public ResponseEntity<Map<String, Object>> rechargeFetchPlan() {
        return response.success("Success", payload());
    }

    private String clientIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
