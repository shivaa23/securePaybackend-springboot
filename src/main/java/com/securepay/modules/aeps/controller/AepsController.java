package com.securepay.modules.aeps.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.modules.aeps.dto.AepsRequest;
import com.securepay.modules.aeps.service.AepsTransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AepsController — ports the aeps module endpoints at their exact paths (doc 05).
 *
 * <p>The transaction endpoint {@code aeps/cashWithdrawl} is wired to {@link AepsTransactionService}
 * (commission → persist → iPay provider → W2 settlement credit on success). Remaining endpoints
 * return the envelope and are filled in per the phased plan (doc 09).</p>
 */
@RestController
public class AepsController extends BaseController {

    private final ResponseFactory response;
    private final AepsTransactionService aepsTransactionService;
    private final ObjectMapper objectMapper;

    public AepsController(ResponseFactory response, AepsTransactionService aepsTransactionService,
                          ObjectMapper objectMapper) {
        this.response = response;
        this.aepsTransactionService = aepsTransactionService;
        this.objectMapper = objectMapper;
    }

    private String clientIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // POST aeps/getAepsTxns
    @PostMapping("aeps/getAepsTxns")
    public ResponseEntity<Map<String, Object>> aepsGetAepsTxns() {
        return response.success("Success", payload());
    }

    // POST aeps/initiateSignupAeps
    @PostMapping("aeps/initiateSignupAeps")
    public ResponseEntity<Map<String, Object>> aepsInitiateSignupAeps() {
        return response.success("Success", payload());
    }

    // POST aeps/outletStatusIpay
    @PostMapping("aeps/outletStatusIpay")
    public ResponseEntity<Map<String, Object>> aepsOutletStatusIpay() {
        return response.success("Success", payload());
    }

    // POST aeps/merchantBiomatricKycIpay
    @PostMapping("aeps/merchantBiomatricKycIpay")
    public ResponseEntity<Map<String, Object>> aepsMerchantBiomatricKycIpay() {
        return response.success("Success", payload());
    }

    // POST aeps/validateSignupAeps
    @PostMapping("aeps/validateSignupAeps")
    public ResponseEntity<Map<String, Object>> aepsValidateSignupAeps() {
        return response.success("Success", payload());
    }

    // POST aeps/getInitiateSchema
    @PostMapping("aeps/getInitiateSchema")
    public ResponseEntity<Map<String, Object>> aepsGetInitiateSchema() {
        return response.success("Success", payload());
    }

    // POST aeps/outletLogin
    @PostMapping("aeps/outletLogin")
    public ResponseEntity<Map<String, Object>> aepsOutletLogin() {
        return response.success("Success", payload());
    }

    // POST aeps/outletLoginStatus
    @PostMapping("aeps/outletLoginStatus")
    public ResponseEntity<Map<String, Object>> aepsOutletLoginStatus() {
        return response.success("Success", payload());
    }

    // POST aeps/balanceEnquiry
    @PostMapping("aeps/balanceEnquiry")
    public ResponseEntity<Map<String, Object>> aepsBalanceEnquiry() {
        return response.success("Success", payload());
    }

    // POST aeps/cashWithdrawl — full transaction orchestration (doc 10 §10.8)
    @PostMapping("aeps/cashWithdrawl")
    public ResponseEntity<Map<String, Object>> aepsCashWithdrawl(
            @AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest request) {
        AepsRequest dto = objectMapper.convertValue(payload(), AepsRequest.class);
        Map<String, Object> result = aepsTransactionService.cashWithdrawl(
                currentUser.user(), dto, request.getHeader("User-Agent"), clientIp(request));
        return response.success("Success", result);
    }

    // POST aeps/getBanks
    @PostMapping("aeps/getBanks")
    public ResponseEntity<Map<String, Object>> aepsGetBanks() {
        return response.success("Success", payload());
    }

    // POST aeps/miniStatement
    @PostMapping("aeps/miniStatement")
    public ResponseEntity<Map<String, Object>> aepsMiniStatement() {
        return response.success("Success", payload());
    }

    // POST aeps/onboardingAeps2
    @PostMapping("aeps/onboardingAeps2")
    public ResponseEntity<Map<String, Object>> aepsOnboardingAeps2() {
        return response.success("Success", payload());
    }

    // POST aeps/onboardingekycFp
    @PostMapping("aeps/onboardingekycFp")
    public ResponseEntity<Map<String, Object>> aepsOnboardingekycFp() {
        return response.success("Success", payload());
    }

    // POST aeps/aeps2Schema
    @PostMapping("aeps/aeps2Schema")
    public ResponseEntity<Map<String, Object>> aepsAeps2Schema() {
        return response.success("Success", payload());
    }

    // POST aeps/validateOtpAeps2
    @PostMapping("aeps/validateOtpAeps2")
    public ResponseEntity<Map<String, Object>> aepsValidateOtpAeps2() {
        return response.success("Success", payload());
    }

    // POST aeps/merchantBiometricKyc
    @PostMapping("aeps/merchantBiometricKyc")
    public ResponseEntity<Map<String, Object>> aepsMerchantBiometricKyc() {
        return response.success("Success", payload());
    }

    // POST aeps/balanceEnquiry2
    @PostMapping("aeps/balanceEnquiry2")
    public ResponseEntity<Map<String, Object>> aepsBalanceEnquiry2() {
        return response.success("Success", payload());
    }

    // POST aeps/cashWithdrawl2
    @PostMapping("aeps/cashWithdrawl2")
    public ResponseEntity<Map<String, Object>> aepsCashWithdrawl2() {
        return response.success("Success", payload());
    }

    // POST aeps/miniStatement2
    @PostMapping("aeps/miniStatement2")
    public ResponseEntity<Map<String, Object>> aepsMiniStatement2() {
        return response.success("Success", payload());
    }

    // POST aeps/getBanksFp
    @PostMapping("aeps/getBanksFp")
    public ResponseEntity<Map<String, Object>> aepsGetBanksFp() {
        return response.success("Success", payload());
    }

    // POST aeps/getStatesFp
    @PostMapping("aeps/getStatesFp")
    public ResponseEntity<Map<String, Object>> aepsGetStatesFp() {
        return response.success("Success", payload());
    }

    // POST aeps/twoFaAeps2
    @PostMapping("aeps/twoFaAeps2")
    public ResponseEntity<Map<String, Object>> aepsTwoFaAeps2() {
        return response.success("Success", payload());
    }

    // POST aeps/addharPayFp
    @PostMapping("aeps/addharPayFp")
    public ResponseEntity<Map<String, Object>> aepsAddharPayFp() {
        return response.success("Success", payload());
    }

    // GET aeps/threeWayReconFp
    @GetMapping("aeps/threeWayReconFp")
    public ResponseEntity<Map<String, Object>> getAepsThreeWayReconFp() {
        return response.success("Success", payload());
    }
}
