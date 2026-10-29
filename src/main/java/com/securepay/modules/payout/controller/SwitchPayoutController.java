package com.securepay.modules.payout.controller;

import com.securepay.common.ratelimit.RateLimit;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Validation;
import com.securepay.modules.auth.service.OtpService;
import com.securepay.modules.payout.entity.PayoutBeneficiary;
import com.securepay.modules.payout.entity.PayoutSender;
import com.securepay.modules.payout.repository.PayoutBeneficiaryRepository;
import com.securepay.modules.payout.repository.PayoutSenderRepository;
import com.securepay.modules.payout.service.PayoutCtx;
import com.securepay.modules.payout.service.PayoutExtras;
import com.securepay.modules.payout.service.PayoutFlow;
import com.securepay.modules.payout.service.PayoutProviders;
import com.securepay.modules.payout.service.PayoutResponseHandler;
import com.securepay.modules.payout.service.PayoutRules;
import com.securepay.modules.payout.service.PayoutSupport;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.securepay.modules.payout.controller.BeneficiaryController.toInt;
import static com.securepay.modules.payout.service.PayoutProviders.uniqid;

/**
 * Port of {@code AbhayamController} and {@code UtkalController}. The two PHP controllers are line-for-line
 * the same flow against different hosts / credentials, so each operation is implemented once and
 * parameterised by {@link Pipe}.
 */
@RestController
public class SwitchPayoutController extends BaseController {

    /** Per-provider differences. */
    private record Pipe(String name, boolean utkal, String senderBase, String beneBase, String txnUrl,
                        String apiLabel, String beneNameKey, String beneDetailsUrl) {
    }

    private static final Pipe ABHAYAM = new Pipe("ABHAYAM", false, PayoutProviders.AB_SENDER,
            PayoutProviders.AB_BENE, PayoutProviders.AB_TXN, "Abhayam", "BeneficiaryName",
            PayoutProviders.AB_BENE + "GetDetails");
    // The source URL for Utkal's GetDetails ends with '=' (kept verbatim).
    private static final Pipe UTKAL = new Pipe("UTKAL", true, PayoutProviders.UT_SENDER,
            PayoutProviders.UT_BENE, PayoutProviders.UT_TXN, "UTKAL", "beneficiary_name",
            PayoutProviders.UT_BENE + "GetDetails=");

    private final ResponseFactory response;
    private final PayoutRules rules;
    private final PayoutSupport support;
    private final PayoutFlow flow;
    private final PayoutExtras extras;
    private final PayoutProviders providers;
    private final PayoutResponseHandler handler;
    private final PayoutSenderRepository senderRepository;
    private final PayoutBeneficiaryRepository beneficiaryRepository;
    private final BeneficiaryController beneficiaryController;

    public SwitchPayoutController(ResponseFactory response, PayoutRules rules, PayoutSupport support,
                                  PayoutFlow flow, PayoutExtras extras, PayoutProviders providers,
                                  PayoutResponseHandler handler, PayoutSenderRepository senderRepository,
                                  PayoutBeneficiaryRepository beneficiaryRepository,
                                  BeneficiaryController beneficiaryController, OtpService otpService) {
        this.response = response;
        this.rules = rules;
        this.support = support;
        this.flow = flow;
        this.extras = extras;
        this.providers = providers;
        this.handler = handler;
        this.senderRepository = senderRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.beneficiaryController = beneficiaryController;
        this.otpService = otpService;
    }

    private final OtpService otpService;

    // ------------------------------------------------------------------ small helpers
    private static String s(Map<String, Object> p, String k) {
        return p.get(k) == null ? null : p.get(k).toString();
    }

    private static String status(Map<String, Object> r) {
        return r == null || r.get("Status") == null ? null : r.get("Status").toString();
    }

    private static String desc(Map<String, Object> r, String fallback) {
        return r != null && r.get("Description") != null ? r.get("Description").toString() : fallback;
    }

    private Map<String, Object> call(Pipe pipe, String url, Map<String, Object> payload) {
        return pipe.utkal() ? providers.utkal(url, payload) : providers.abhayam(url, payload);
    }

    private ResponseEntity<Map<String, Object>> senderRoleDenied(User user) {
        return PayoutSupport.hasRole(user, "ret", "dd", "api") ? null
                : response.error("Unauthorised Action", "", HttpStatus.FORBIDDEN);
    }

    private static BigDecimal decimal(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return new BigDecimal(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ getSender4 / getSender5 / getSenderUpi2
    private ResponseEntity<Map<String, Object>> getSender(User user, Pipe pipe, String beneType, boolean fetchBenes) {
        ResponseEntity<Map<String, Object>> denied = senderRoleDenied(user);
        if (denied != null) {
            return denied;
        }
        String mobile = s(payload(), "mobile_number");
        if (mobile == null || mobile.isBlank()) {
            return response.error("Mobile number required", "", HttpStatus.FORBIDDEN);
        }
        PayoutSender existing = senderRepository.findFirstByMobileNumberAndPipeAndIsActive(mobile, pipe.name(), 1)
                .orElse(null);
        if (existing != null) {
            return response.success("Success", support.withBeneficiaries(existing, beneType));
        }
        Map<String, Object> check = providers.auth(pipe.utkal());
        check.put("SenderMobile", mobile);
        check.put("RequestID", uniqid("req_"));
        Map<String, Object> resp = call(pipe, pipe.senderBase() + "Check", check);
        if (!"Success".equals(status(resp))) {
            return response.success("Sender Not Found", "");
        }
        PayoutSender sender = senderRepository.findFirstByMobileNumberAndPipe(mobile, pipe.name())
                .orElseGet(PayoutSender::new);
        sender.setMobileNumber(mobile);
        sender.setPipe(pipe.name());
        sender.setSenderName(resp.get("SenderName") != null ? resp.get("SenderName").toString() : "");
        sender.setKycStatus("verified");
        sender.setUserId(user.getId());
        sender.setRemLimit(decimal(resp.get("SenderBalance")));
        sender.setIsActive(1);
        sender = senderRepository.save(sender);

        if (fetchBenes) {
            Map<String, Object> bp = providers.auth(pipe.utkal());
            bp.put("SenderMobile", mobile);
            bp.put("RequestID", uniqid("req_"));
            Map<String, Object> br = call(pipe, pipe.beneDetailsUrl(), bp);
            if ("Success".equals(status(br)) && br.get("Data") instanceof List<?> list && !list.isEmpty()) {
                for (Object o : list) {
                    if (!(o instanceof Map<?, ?> bene)) {
                        continue;
                    }
                    String acc = bene.get("AccountNo") == null ? null : bene.get("AccountNo").toString();
                    PayoutBeneficiary b = beneficiaryRepository
                            .findFirstBySenderIdAndAccountNumberAndType(sender.getId(), acc, pipe.name())
                            .orElseGet(PayoutBeneficiary::new);
                    b.setSenderId(sender.getId());
                    b.setAccountNumber(acc);
                    b.setType(pipe.name());
                    b.setBeneficiaryName(bene.get(pipe.beneNameKey()) == null ? null
                            : bene.get(pipe.beneNameKey()).toString());
                    b.setIfscCode(bene.get("Ifsc") == null ? null : bene.get("Ifsc").toString());
                    b.setBankName(bene.get("BankName") == null ? null : bene.get("BankName").toString());
                    b.setApiId(bene.get("BeneficiaryId") == null ? null : bene.get("BeneficiaryId").toString());
                    b.setMobileNumber(mobile);
                    beneficiaryRepository.save(b);
                }
            }
        }
        return response.success("Success", support.withBeneficiaries(sender, beneType));
    }

    // POST payout/getSender4
    @PostMapping("payout/getSender4")
    public ResponseEntity<Map<String, Object>> getSender4(@AuthenticationPrincipal CurrentUser cu) {
        return getSender(cu.user(), ABHAYAM, "ABHAYAM", true);
    }

    // POST payout/getSender5
    @PostMapping("payout/getSender5")
    public ResponseEntity<Map<String, Object>> getSender5(@AuthenticationPrincipal CurrentUser cu) {
        return getSender(cu.user(), UTKAL, "UTKAL", true);
    }

    // POST payout/getSenderUpi2
    @PostMapping("payout/getSenderUpi2")
    public ResponseEntity<Map<String, Object>> getSenderUpi2(@AuthenticationPrincipal CurrentUser cu) {
        return getSender(cu.user(), UTKAL, "UTKALUPI", false);
    }

    // ------------------------------------------------------------------ register remitter
    private ResponseEntity<Map<String, Object>> registerRemitter(User user, Pipe pipe) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("mobile_number").required().digits(10);
        v.field("name").required().string().max(50);
        if (v.fails()) {
            return support.validationAsMessage(v, 400);
        }
        String fName = PayoutSupport.lettersOnly(s(p, "name"));
        Map<String, Object> payload = providers.auth(pipe.utkal());
        payload.put("SenderMobile", s(p, "mobile_number"));
        payload.put("SenderName", fName);
        payload.put("RequestID", uniqid("req_"));
        Map<String, Object> resp = call(pipe, pipe.senderBase() + "Register", payload);
        if (status(resp) == null) {
            return response.error("Something went wrong", List.of(), HttpStatus.BAD_REQUEST);
        }
        switch (status(resp)) {
            case "Success" -> {
                PayoutSender sender = senderRepository
                        .findFirstByMobileNumberAndPipe(s(p, "mobile_number"), pipe.name())
                        .orElseGet(PayoutSender::new);
                sender.setSenderName(resp.get("SenderName") != null ? resp.get("SenderName").toString() : fName);
                sender.setMobileNumber(s(p, "mobile_number"));
                sender.setPipe(pipe.name());
                if (pipe.utkal()) {
                    sender.setAadhar(s(p, "aadhar"));
                    sender.setPan(s(p, "pan"));
                }
                sender.setKycStatus("pending");
                sender.setIsActive(0);
                sender.setUserId(user.getId());
                senderRepository.save(sender);
                return response.success(desc(resp, "OTP sent successfully"), resp.get("ReferenceId"));
            }
            case "Failed" -> {
                return response.error(desc(resp, "Failed"), List.of(), HttpStatus.BAD_REQUEST);
            }
            default -> {
                return response.error("Something went wrong", List.of(), HttpStatus.BAD_REQUEST);
            }
        }
    }

    // POST payout/registerremitter3
    @PostMapping("payout/registerremitter3")
    public ResponseEntity<Map<String, Object>> remitterLogin3(@AuthenticationPrincipal CurrentUser cu) {
        return registerRemitter(cu.user(), ABHAYAM);
    }

    // POST payout/registerRemitter4
    @PostMapping("payout/registerRemitter4")
    public ResponseEntity<Map<String, Object>> registerRemitter4(@AuthenticationPrincipal CurrentUser cu) {
        return registerRemitter(cu.user(), UTKAL);
    }

    // ------------------------------------------------------------------ verify remitter
    private ResponseEntity<Map<String, Object>> verifyRemitter(User user, Pipe pipe) {
        Map<String, Object> p = payload();
        Validation v = rules.verifySender(p);
        if (v.fails()) {
            return support.validationFailed(v, 422);
        }
        if (!PayoutSupport.hasRole(user, "ret", "dd", "api")) {
            return response.error("Unauthorized Action", null, HttpStatus.FORBIDDEN);
        }
        PayoutSender sender = senderRepository.findFirstByMobileNumberAndPipe(s(p, "mobile_number"), pipe.name())
                .orElse(null);
        if (sender == null) {
            return response.error("Sender not found", null, HttpStatus.NOT_FOUND);
        }
        Map<String, Object> payload = providers.auth(pipe.utkal());
        payload.put("ReferenceId", s(p, "otp_ref"));
        payload.put("OTP", s(p, "otp"));
        payload.put("RequestID", uniqid("req_"));
        Map<String, Object> resp = call(pipe, pipe.senderBase() + "Verify", payload);
        if (status(resp) == null) {
            return response.error("Invalid API response", resp, HttpStatus.BAD_REQUEST);
        }
        if ("Success".equals(status(resp))) {
            sender.setKycStatus("verified");
            sender.setIsActive(1);
            senderRepository.save(sender);
            return response.success(desc(resp, "Sender Verified Successfully"), resp);
        }
        return response.error(desc(resp, "Verification Failed"), resp, HttpStatus.BAD_REQUEST);
    }

    // POST payout/verifyRemitterLogin3
    @PostMapping("payout/verifyRemitterLogin3")
    public ResponseEntity<Map<String, Object>> verifyRemitterLogin3(@AuthenticationPrincipal CurrentUser cu) {
        return verifyRemitter(cu.user(), ABHAYAM);
    }

    // POST payout/verifyRemitterLogin4
    @PostMapping("payout/verifyRemitterLogin4")
    public ResponseEntity<Map<String, Object>> verifyRemitterLogin4(@AuthenticationPrincipal CurrentUser cu) {
        return verifyRemitter(cu.user(), UTKAL);
    }

    // ------------------------------------------------------------------ banks
    private ResponseEntity<Map<String, Object>> getBanks(User user, Pipe pipe) {
        if (!PayoutSupport.hasRole(user, "ret", "dd", "api")) {
            return response.error("Unauthorized Action", "", HttpStatus.FORBIDDEN);
        }
        Map<String, Object> payload = providers.auth(pipe.utkal());
        payload.put("RequestID", uniqid("req_"));
        Map<String, Object> resp = call(pipe, pipe.beneBase() + "GetBankDetails", payload);
        if (status(resp) == null) {
            return response.error("Invalid response from " + pipe.apiLabel() + " API", List.of(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (!"Success".equals(status(resp))) {
            return response.error(desc(resp, "Failed"), List.of(), HttpStatus.BAD_REQUEST);
        }
        return response.success("Bank List Fetched Successfully",
                resp.get("Data") != null ? resp.get("Data") : List.of());
    }

    // POST payout/getAbhayamBanks
    @PostMapping("payout/getAbhayamBanks")
    public ResponseEntity<Map<String, Object>> getAbhayamBanks(@AuthenticationPrincipal CurrentUser cu) {
        return getBanks(cu.user(), ABHAYAM);
    }

    // POST payout/getUtkalBanks
    @PostMapping("payout/getUtkalBanks")
    public ResponseEntity<Map<String, Object>> getUtkalBanks(@AuthenticationPrincipal CurrentUser cu) {
        return getBanks(cu.user(), UTKAL);
    }

    // ------------------------------------------------------------------ beneficiaries
    private ResponseEntity<Map<String, Object>> addBeneficiary(User user, Pipe pipe) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("mobile_number").required().digits(10);
        v.field("bene_name").required().string().max(80);
        v.field("bene_account").required().numeric();
        v.field("ifsc_code").required().string().max(15);
        v.field("bank_name").required().string().max(150);
        v.field("bank_code").required().string().max(20);
        if (v.fails()) {
            return response.error("Validation Failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!PayoutSupport.hasRole(user, "dd", "ret", "api")) {
            return response.error("Unauthorized action", List.of(), HttpStatus.FORBIDDEN);
        }
        PayoutSender sender = senderRepository.findFirstByMobileNumberAndPipe(s(p, "mobile_number"), pipe.name())
                .orElse(null);
        if (sender == null) {
            return response.error("Sender not found", List.of(), HttpStatus.CONFLICT);
        }
        String bene = PayoutSupport.alnumName(s(p, "bene_name"));
        Map<String, Object> payload = providers.auth(pipe.utkal());
        payload.put("SenderMobile", s(p, "mobile_number"));
        payload.put("BeneficiaryName", bene);
        payload.put("AccountNo", s(p, "bene_account"));
        payload.put("Ifsc", s(p, "ifsc_code").toUpperCase());
        payload.put("BankCode", s(p, "bank_code").toUpperCase());
        payload.put("BankName", s(p, "bank_name"));
        payload.put("RequestID", uniqid("req_"));
        payload.put("Rfu1", "");
        payload.put("Rfu2", "");
        payload.put("Rfu3", "");
        Map<String, Object> resp = call(pipe, pipe.beneBase() + "Add", payload);
        if (status(resp) == null) {
            return response.error("Invalid response from " + pipe.apiLabel() + " API", List.of(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (!"Success".equals(status(resp))) {
            return response.error(desc(resp, "Failed"), List.of(), HttpStatus.BAD_REQUEST);
        }
        if (beneficiaryRepository.findFirstByMobileNumberAndAccountNumberAndIfscCodeAndType(
                s(p, "mobile_number"), s(p, "bene_account"), s(p, "ifsc_code"), pipe.name()).isPresent()) {
            return response.error("Beneficiary already exists", List.of(), HttpStatus.CONFLICT);
        }
        PayoutBeneficiary b = new PayoutBeneficiary();
        b.setBeneficiaryName(bene);
        b.setIfscCode(s(p, "ifsc_code").toUpperCase());
        b.setAccountNumber(s(p, "bene_account"));
        b.setBankName(s(p, "bank_name"));
        b.setType(pipe.name());
        b.setApiId(resp.get("BenficiaryId") == null ? null : resp.get("BenficiaryId").toString());
        b.setSenderId(sender.getId());
        b.setMobileNumber(s(p, "mobile_number"));
        b.setIsVerified(p.get("is_verified") == null ? null : toInt(p.get("is_verified")));
        b = beneficiaryRepository.save(b);
        return response.success("Beneficiary Added Successfully", b);
    }

    // POST payout/addBeneficiaryAbhayam
    @PostMapping("payout/addBeneficiaryAbhayam")
    public ResponseEntity<Map<String, Object>> addBeneficiaryAbhayam(@AuthenticationPrincipal CurrentUser cu) {
        return addBeneficiary(cu.user(), ABHAYAM);
    }

    // POST payout/addBeneficiaryUtkal
    @PostMapping("payout/addBeneficiaryUtkal")
    public ResponseEntity<Map<String, Object>> addBeneficiaryUtkal(@AuthenticationPrincipal CurrentUser cu) {
        return addBeneficiary(cu.user(), UTKAL);
    }

    private ResponseEntity<Map<String, Object>> deleteBeneficiary(User user, Pipe pipe) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("mobile_number").required().digits(10);
        v.field("bene_id").required();
        if (v.fails()) {
            return response.error("Validation Failed", v.errors(), HttpStatus.BAD_REQUEST);
        }
        if (!PayoutSupport.hasRole(user, "adm", "sadm", "ret", "api")) {
            return response.error("Unauthorized action", List.of(), HttpStatus.FORBIDDEN);
        }
        Map<String, Object> payload = providers.auth(pipe.utkal());
        payload.put("SenderMobile", s(p, "mobile_number"));
        payload.put("RequestID", uniqid("req_"));
        payload.put("BeneficiaryId", s(p, "bene_id"));
        Map<String, Object> resp = call(pipe, pipe.beneBase() + "Delete", payload);
        if (resp == null || resp.isEmpty() || status(resp) == null) {
            return response.error("Invalid response from " + pipe.apiLabel() + " API", List.of(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (!"Success".equals(status(resp))) {
            return response.error(desc(resp, "Failed"), List.of(), HttpStatus.BAD_REQUEST);
        }
        beneficiaryRepository.deleteByMobileAndApiId(s(p, "mobile_number"), s(p, "bene_id"));
        return response.success("Beneficiary Deleted Successfully", List.of());
    }

    // POST payout/deleteBeneficiaryAbhayam
    @PostMapping("payout/deleteBeneficiaryAbhayam")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteBeneficiaryAbhayam(@AuthenticationPrincipal CurrentUser cu) {
        return deleteBeneficiary(cu.user(), ABHAYAM);
    }

    // POST payout/deleteBeneficiaryUtkal
    @PostMapping("payout/deleteBeneficiaryUtkal")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteBeneficiaryUtkal(@AuthenticationPrincipal CurrentUser cu) {
        return deleteBeneficiary(cu.user(), UTKAL);
    }

    // POST payout/createBeneficiaryUpi2 — same as createBeneficiary but stored as type UTKALUPI
    @PostMapping("payout/createBeneficiaryUpi2")
    public ResponseEntity<Map<String, Object>> createBeneficiaryUpi2() {
        return beneficiaryController.create("UTKALUPI");
    }

    // POST payout/getAllVpa
    @PostMapping("payout/getAllVpa")
    public ResponseEntity<Map<String, Object>> getAllVpa(@AuthenticationPrincipal CurrentUser cu) {
        if (!PayoutSupport.hasRole(cu.user(), "adm", "sadm", "ret", "dd", "api")) {
            return response.error("Unauthorized", List.of(), HttpStatus.FORBIDDEN);
        }
        String senderId = s(payload(), "sender_id");
        List<PayoutBeneficiary> rows = senderId != null && !senderId.isBlank()
                ? beneficiaryRepository.findByTypeAndSenderIdOrderByIdDesc("UTKALUPI", Long.valueOf(senderId.trim()))
                : beneficiaryRepository.findByTypeOrderByIdDesc("UTKALUPI");
        List<Map<String, Object>> out = new ArrayList<>();
        for (PayoutBeneficiary b : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("sender_id", b.getSenderId());
            m.put("vpa", b.getAccountNumber());
            m.put("is_verified", b.getIsVerified());
            m.put("created_at", b.getCreatedAt());
            out.add(m);
        }
        return response.success("VPA list fetched", out);
    }

    // ------------------------------------------------------------------ payouts
    private ResponseEntity<Map<String, Object>> payOutSwitch(User user, HttpServletRequest request, Pipe pipe,
                                                             String uuidAction) {
        PayoutCtx c = PayoutCtx.of(payload(), request);
        Validation v = rules.payOutAbhayam(c.p(), user);
        if (v.fails()) {
            return response.error("Validation failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        flow.consumeUuid(c, uuidAction);
        PayoutFlow.Spec spec = new PayoutFlow.Spec(
                false,
                mobile -> senderRepository.findFirstByMobileNumberAndPipe(mobile, pipe.name()).orElse(null),
                null,
                false,
                (txn, service, u, sender) -> {
                    Map<String, Object> payload = providers.auth(pipe.utkal());
                    payload.put("RequestID", txn.getTxnId());
                    payload.put("ServiceId", pipe.utkal() ? (Object) 27 : "27");
                    payload.put("SenderMobile", c.s("mobile_number"));
                    payload.put("BeneficiaryId", c.s("bene_id"));
                    payload.put("Amount", c.s("amount"));
                    payload.put("AdharNo", "123123123123");
                    payload.put("PanNo", u.getPan() == null ? "" : u.getPan());
                    payload.put("Lat", c.s("latitude") == null ? "" : c.s("latitude"));
                    payload.put("Long", c.s("longitude") == null ? "" : c.s("longitude"));
                    return call(pipe, pipe.txnUrl(), payload);
                },
                handler::handleResponseAbhyam);
        return flow.run(user, c, spec);
    }

    // POST payments/v2/payOutAbhayam (throttle:1,0.5)
    @PostMapping("payments/v2/payOutAbhayam")
    @RateLimit(requests = 1, seconds = 30)
    public ResponseEntity<Map<String, Object>> payOutAbhayam(@AuthenticationPrincipal CurrentUser cu,
                                                             HttpServletRequest request) {
        return payOutSwitch(cu.user(), request, ABHAYAM, "payOut2");
    }

    // POST payments/v2/payOutUtkal (throttle:1,0.5)
    @PostMapping("payments/v2/payOutUtkal")
    @RateLimit(requests = 1, seconds = 30)
    public ResponseEntity<Map<String, Object>> payOutUtkal(@AuthenticationPrincipal CurrentUser cu,
                                                           HttpServletRequest request) {
        return payOutSwitch(cu.user(), request, UTKAL, "payOutUtkal");
    }

    // POST payments/v2/upiUtkal (throttle:1,0.5)
    @PostMapping("payments/v2/upiUtkal")
    @RateLimit(requests = 1, seconds = 30)
    public ResponseEntity<Map<String, Object>> upiUtkal(@AuthenticationPrincipal CurrentUser cu,
                                                        HttpServletRequest request) {
        User user = cu.user();
        PayoutCtx c = PayoutCtx.of(payload(), request);
        Validation v = rules.upiUtkal(c.p(), user);
        if (v.fails()) {
            return response.error("Validation failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        flow.consumeUuid(c, "payOutUtkal");
        PayoutFlow.Spec spec = new PayoutFlow.Spec(
                false,
                mobile -> senderRepository.findFirstByMobileNumberAndPipe(mobile, "UTKAL")
                        .or(() -> senderRepository.findFirstByMobileNumberAndPipe(mobile, "ABHAYAM")).orElse(null),
                (ServiceEntity svc) -> "UTKAL".equals(svc.getRoute()) || "ABHAYAM".equals(svc.getRoute()),
                false,
                (txn, service, u, sender) -> {
                    boolean utkal = "UTKAL".equals(service.getRoute());
                    Map<String, Object> payload = providers.auth(utkal);
                    payload.put("RequestID", txn.getTxnId());
                    payload.put("ServiceId", 37);
                    payload.put("VPA", c.s("account_number"));
                    payload.put("Amount", c.s("amount"));
                    payload.put("MobileNumber", c.s("mobile_number"));
                    payload.put("Name", c.s("beneficiary_name"));
                    payload.put("Lat", c.s("latitude") != null ? c.s("latitude") : "22.753");
                    payload.put("Long", c.s("longitude") != null ? c.s("longitude") : "78.773");
                    payload.put("EmailId", c.s("email") != null ? c.s("email") : "info@test.com");
                    payload.put("Address", c.s("address") != null ? c.s("address") : "India");
                    return utkal ? providers.utkal(PayoutProviders.UT_UPI + "transaction", payload)
                            : providers.abhayam(PayoutProviders.AB_UPI_TXN, payload);
                },
                handler::handleResponseAbhyam);
        return flow.run(user, c, spec);
    }

    // POST payout/upiVerification
    @PostMapping("payout/upiVerification")
    public ResponseEntity<Map<String, Object>> upiVerification(@AuthenticationPrincipal CurrentUser cu,
                                                               HttpServletRequest request) {
        User user = cu.user();
        PayoutCtx c = PayoutCtx.of(payload(), request);
        Validation v = rules.upiVerification(c.p(), user);
        if (v.fails()) {
            return response.error("Validation error", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return extras.upiVerification(user, c);
    }

    // POST qr/initiateDynamic
    @PostMapping("qr/initiateDynamic")
    public ResponseEntity<Map<String, Object>> initiateDynamic(@AuthenticationPrincipal CurrentUser cu,
                                                               HttpServletRequest request) {
        return extras.initiateDynamicQr(cu.user(), PayoutCtx.of(payload(), request));
    }
}
