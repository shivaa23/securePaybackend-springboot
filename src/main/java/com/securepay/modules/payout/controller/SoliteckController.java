package com.securepay.modules.payout.controller;

import com.securepay.common.exception.ApiException;
import com.securepay.common.ratelimit.RateLimit;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.util.Json;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Validation;
import com.securepay.modules.auth.service.OtpService;
import com.securepay.modules.payout.entity.PayoutBeneficiary;
import com.securepay.modules.payout.entity.PayoutSender;
import com.securepay.modules.payout.repository.PayoutBeneficiaryRepository;
import com.securepay.modules.payout.repository.PayoutSenderRepository;
import com.securepay.modules.payout.service.PayoutCtx;
import com.securepay.modules.payout.service.PayoutFlow;
import com.securepay.modules.payout.service.PayoutProviders;
import com.securepay.modules.payout.service.PayoutResponseHandler;
import com.securepay.modules.payout.service.PayoutRules;
import com.securepay.modules.payout.service.PayoutSupport;
import com.securepay.modules.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.securepay.modules.payout.controller.BeneficiaryController.toInt;
import static com.securepay.modules.payout.service.PayoutProviders.SOL;
import static com.securepay.modules.payout.service.PayoutProviders.uniqid;

/** Port of {@code Payout/Controllers/SoliteckController} (pipe = SOLITECK, operator 81). */
@RestController
public class SoliteckController extends BaseController {

    private static final String TYPE = "SOLITECK";

    private final ResponseFactory response;
    private final PayoutRules rules;
    private final PayoutSupport support;
    private final PayoutFlow flow;
    private final PayoutProviders providers;
    private final PayoutResponseHandler handler;
    private final PayoutSenderRepository senderRepository;
    private final PayoutBeneficiaryRepository beneficiaryRepository;
    private final OtpService otpService;

    public SoliteckController(ResponseFactory response, PayoutRules rules, PayoutSupport support, PayoutFlow flow,
                              PayoutProviders providers, PayoutResponseHandler handler,
                              PayoutSenderRepository senderRepository,
                              PayoutBeneficiaryRepository beneficiaryRepository, OtpService otpService) {
        this.response = response;
        this.rules = rules;
        this.support = support;
        this.flow = flow;
        this.providers = providers;
        this.handler = handler;
        this.senderRepository = senderRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.otpService = otpService;
    }

    private static String s(Map<String, Object> p, String k) {
        return p.get(k) == null ? null : p.get(k).toString();
    }

    private static boolean has(Map<String, Object> p, String k) {
        return p.get(k) != null && !p.get(k).toString().isBlank();
    }

    private static String code(Map<String, Object> r) {
        return r == null || r.get("responseCode") == null ? null : r.get("responseCode").toString();
    }

    /** {@code SoliteckController::handleResponse}. */
    ResponseEntity<Map<String, Object>> handleResponse(Map<String, Object> r) {
        if (code(r) == null) {
            return response.error("Something went wrong", List.of(), HttpStatus.BAD_REQUEST);
        }
        switch (code(r)) {
            case "200", "201", "204" -> {
                Object data = r.get("responseData") == null ? null : decode(r.get("responseData").toString());
                return response.success(Objects.toString(r.get("responseMessage"), null), data,
                        HttpStatus.valueOf(Integer.parseInt(code(r))));
            }
            case "401" -> {
                return response.error(Objects.toString(r.get("responseMessage"), "Token expired"), List.of(),
                        HttpStatus.UNAUTHORIZED);
            }
            default -> {
                return response.error(Objects.toString(r.get("responseMessage"), "Something1 went wrong"),
                        List.of(), HttpStatus.BAD_REQUEST);
            }
        }
    }

    /** json_decode($string, true): object/array, else null. */
    static Object decode(String s) {
        Object o = Json.readMap(s);
        return o != null ? o : Json.readList(s);
    }

    // POST payout/getSender2
    @PostMapping("payout/getSender2")
    public ResponseEntity<Map<String, Object>> getSender2(@AuthenticationPrincipal CurrentUser cu) {
        if (!PayoutSupport.hasRole(cu.user(), "ret", "dd", "api")) {
            return response.error("Unauthorized action", List.of(), HttpStatus.FORBIDDEN);
        }
        String mobile = s(payload(), "mobile_number");
        if (mobile == null || mobile.isBlank()) {
            return response.error("Mobile number is required", List.of(), HttpStatus.BAD_REQUEST);
        }
        PayoutSender sender = senderRepository.findFirstByMobileNumberAndPipe(mobile, TYPE).orElse(null);
        if (sender == null) {
            return response.error("Sender not found", List.of(), HttpStatus.NOT_FOUND);
        }
        return response.success("Success", support.withBeneficiaries(sender, TYPE));
    }

    // POST payout/getSender3
    @PostMapping("payout/getSender3")
    public ResponseEntity<Map<String, Object>> getSender3(@AuthenticationPrincipal CurrentUser cu) {
        User user = cu.user();
        if (!PayoutSupport.hasRole(user, "ret", "dd", "api")) {
            return response.error("Unauthorised Action", "", HttpStatus.FORBIDDEN);
        }
        String mobile = s(payload(), "mobile_number");
        if (mobile == null || mobile.isBlank()) {
            return response.error("Mobile number required", "", HttpStatus.FORBIDDEN);
        }
        PayoutSender sender = senderRepository.findFirstByMobileNumberAndPipe(mobile, TYPE).orElse(null);
        if (sender == null) {
            return response.success("Sender Not Found", "");
        }
        support.withBeneficiaries(sender, TYPE);
        if (sender.getIsActive() != null && sender.getIsActive() == 0) {
            var otpRef = support.sendMobVerifyOtp(user, sender.getMobileNumber());
            if (otpRef.isEmpty()) {
                return response.error("Template not found", List.of(), HttpStatus.FORBIDDEN);
            }
            Map<String, Object> errs = new LinkedHashMap<>();
            errs.put("mobile_number", sender.getMobileNumber());
            errs.put("otp_ref", otpRef.get());
            return response.error("The number is inactive", errs, HttpStatus.FORBIDDEN);
        }
        if ("pending".equals(sender.getKycStatus())) {
            return response.success("Sender Not Verified", "");
        }
        return response.success("Payout transactions fetched", sender);
    }

    private Map<String, Object> remitterPayload(String mobile, String fName, String lName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ClientReq", uniqid("req_"));
        m.put("RemitterMobile", mobile);
        m.put("RemiFName", fName);
        m.put("RemiLName", lName);
        m.put("Token", providers.soliteckToken());
        return m;
    }

    // POST payout/verifyRemitterLogin
    @PostMapping("payout/verifyRemitterLogin")
    public ResponseEntity<Map<String, Object>> verifyRemitterLogin(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = rules.verifySender(p);
        if (v.fails()) {
            return support.validationFailed(v, 422);
        }
        User user = cu.user();
        if (!PayoutSupport.hasRole(user, "ret", "dd", "api")) {
            return response.error("Unauthorized Action", null, HttpStatus.FORBIDDEN);
        }
        PayoutSender sender = senderRepository.findFirstByMobileNumberAndPipe(s(p, "mobile_number"), TYPE).orElse(null);
        if (sender == null) {
            return response.error("Sender not found", null, HttpStatus.NOT_FOUND);
        }
        OtpService.Result r = otpService.verifyOtp(user, s(p, "otp_ref"), s(p, "otp"));
        if (!r.success()) {
            return response.error(r.message(), null, HttpStatus.FORBIDDEN);
        }
        String[] parts = sender.getSenderName().trim().split(" ");
        String fName = parts[0];
        String lName = parts.length > 1 ? parts[1] : "";
        Map<String, Object> resp = providers.soliteck(SOL + "Payout/PayoutRemitterLogIn-withoutOtp",
                remitterPayload(sender.getMobileNumber(), fName, lName));
        if (code(resp) == null) {
            return response.error("Something went wrong", resp, HttpStatus.BAD_REQUEST);
        }
        switch (code(resp)) {
            case "200", "201" -> {
                Object remitter = decode(Objects.toString(resp.get("responseData"), ""));
                sender.setKycStatus("verified");
                sender.setIsActive(1);
                senderRepository.save(sender);
                return response.success(Objects.toString(resp.get("responseMessage"), "Remitter verified successfully"),
                        remitter, HttpStatus.valueOf(Integer.parseInt(code(resp))));
            }
            case "401" -> {
                return response.error(Objects.toString(resp.get("responseMessage"), "Token expired"), null,
                        HttpStatus.FORBIDDEN);
            }
            default -> {
                return response.error(Objects.toString(resp.get("responseMessage"), "Something went wrong"), null,
                        HttpStatus.BAD_REQUEST);
            }
        }
    }

    private Validation remitterNameRules(Map<String, Object> p) {
        Validation v = Validation.of(p);
        v.field("mobile_number").required().digits(10);
        v.field("f_name").required().string().max(50);
        v.field("l_name").required().string().max(50);
        return v;
    }

    // POST payout/remitterLogin
    @PostMapping("payout/remitterLogin")
    public ResponseEntity<Map<String, Object>> remitterLogin(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = remitterNameRules(p);
        if (v.fails()) {
            return support.validationAsMessage(v, 400);
        }
        User user = cu.user();
        String fName = PayoutSupport.lettersOnly(s(p, "f_name"));
        String lName = PayoutSupport.lettersOnly(s(p, "l_name"));
        Map<String, Object> resp = providers.soliteck(SOL + "Payout/PayoutRemitterLogIn-withoutOtp",
                remitterPayload(s(p, "mobile_number"), fName, lName));
        if (code(resp) == null) {
            return response.error("Something went wrong", List.of(), HttpStatus.BAD_REQUEST);
        }
        switch (code(resp)) {
            case "200", "201" -> {
                Map<String, Object> remitter = Json.readMap(Objects.toString(resp.get("responseData"), ""));
                PayoutSender n = new PayoutSender();
                n.setSenderName(remitter == null ? null : Objects.toString(remitter.get("RemitterName"), null));
                n.setMobileNumber(remitter == null ? null : Objects.toString(remitter.get("RemitterMobile"), null));
                n.setKycStatus("verified");
                n.setIsActive(1);
                n.setPipe(TYPE);
                n.setUserId(user.getId());
                senderRepository.save(n);
                return response.success(Objects.toString(resp.get("responseMessage"), null), remitter,
                        HttpStatus.valueOf(Integer.parseInt(code(resp))));
            }
            case "401" -> {
                return response.error(Objects.toString(resp.get("responseMessage"), "Token expired"), List.of(),
                        HttpStatus.FORBIDDEN);
            }
            default -> {
                return response.error(Objects.toString(resp.get("responseMessage"), "Something went wrong"),
                        List.of(), HttpStatus.BAD_REQUEST);
            }
        }
    }

    // POST payout/remitterLogin2
    @PostMapping("payout/remitterLogin2")
    public ResponseEntity<Map<String, Object>> remitterLogin2(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = remitterNameRules(p);
        if (v.fails()) {
            return support.validationAsMessage(v, 400);
        }
        User user = cu.user();
        if (!PayoutSupport.hasRole(user, "ret", "dd", "api")) {
            return response.error("Unauthorised Action", "", HttpStatus.FORBIDDEN);
        }
        String fName = PayoutSupport.lettersOnly(s(p, "f_name"));
        String lName = PayoutSupport.lettersOnly(s(p, "l_name"));
        PayoutSender sender = new PayoutSender();
        sender.setUserId(user.getId());
        sender.setSenderName((fName + " " + lName).trim());
        sender.setEmail(p.get("email") != null ? s(p, "email") : user.getEmail());
        sender.setPipe(TYPE);
        sender.setMobileNumber(s(p, "mobile_number"));
        sender = senderRepository.save(sender);
        var otpRef = support.sendMobVerifyOtp(user, sender.getMobileNumber());
        if (otpRef.isEmpty()) {
            return response.jsonResponse("1001", "Template not found", null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sender", sender);
        data.put("otp_ref", otpRef.get());
        return response.success("Sender Registered Successfully", data);
    }

    // POST payout/remitterRegistration — the source returns the raw provider response
    @PostMapping("payout/remitterRegistration")
    public ResponseEntity<Map<String, Object>> remitterRegistration() {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("db_status").required().in("0", "1");
        v.field("vendor_id").required().integer();
        v.field("otp").required().string();
        boolean newCustomer = "0".equals(s(p, "db_status"));
        v.field("fname").requiredIf(newCustomer).string();
        v.field("lname").requiredIf(newCustomer).string();
        v.field("pincode").requiredIf(newCustomer).digits(6);
        v.field("mobile_number").required().numeric().regex("^[6-9][0-9]{9}$");
        if (v.fails()) {
            return support.validationAsMessage(v, 400);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("RemitterMobile", s(p, "mobile_number"));
        payload.put("Token", providers.soliteckToken());
        payload.put("DBStatus", s(p, "db_status"));
        payload.put("VendorId", s(p, "vendor_id"));
        payload.put("OTP", s(p, "otp"));
        payload.put("ClientReq", uniqid("req_"));
        if (newCustomer) {
            payload.put("FName", s(p, "fname"));
            payload.put("LName", s(p, "lname"));
            payload.put("Pincode", s(p, "pincode"));
        }
        Map<String, Object> resp = providers.soliteck("https://api-uat.soliteck.in/uat/api/PayoutTxn/RemitterRegistration", payload);
        return ResponseEntity.ok(resp);
    }

    // POST payout/getBanks
    @PostMapping("payout/getBanks")
    public ResponseEntity<Map<String, Object>> getBanks() {
        Map<String, Object> resp = providers.soliteck(SOL + "Payout/GetDMTBanksForAPIUser",
                "nileshnathalalbujad.11.2083@SoliteckPay");
        if (code(resp) != null) {
            if ("200".equals(code(resp))) {
                return response.success(Objects.toString(resp.get("responseMessage"), null),
                        decode(Objects.toString(resp.get("responseData"), "")));
            } else if ("401".equals(code(resp))) {
                return response.error(Objects.toString(resp.get("responseMessage"), "Token expired"), List.of(),
                        HttpStatus.UNAUTHORIZED);
            }
            return response.error(Objects.toString(resp.get("responseMessage"), "Something went wrong"), List.of(),
                    HttpStatus.BAD_REQUEST);
        }
        return response.error("Something went wrong", List.of(), HttpStatus.BAD_REQUEST);
    }

    // POST payout/getIfsc
    @PostMapping("payout/getIfsc")
    public ResponseEntity<Map<String, Object>> getIfsc() {
        String bankCode = s(payload(), "bank_code");
        if (bankCode == null || bankCode.isBlank()) {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("error", true);
            raw.put("message", "Bank code is required");
            return ResponseEntity.ok(raw);
        }
        Map<String, Object> resp = providers.soliteck(SOL + "Payout/ApiUserGetMasterIFSCForBank", bankCode);
        if (code(resp) != null) {
            String msg = Objects.toString(resp.get("responseMessage"), null);
            switch (code(resp)) {
                case "200" -> {
                    return response.success(msg, resp.get("responseData"));
                }
                case "401" -> {
                    return response.error(msg, resp.get("responseData"), HttpStatus.UNAUTHORIZED);
                }
                default -> {
                    return response.error(msg, resp.get("responseData"), HttpStatus.FORBIDDEN);
                }
            }
        }
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("error", true);
        raw.put("message", "Something went wrong");
        return ResponseEntity.ok(raw);
    }

    // POST payout/addBeneficiary
    @PostMapping("payout/addBeneficiary")
    public ResponseEntity<Map<String, Object>> addBeneficiary(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        Map<String, String> m = new java.util.HashMap<>();
        m.put("mobile_number.required", "Mobile number is required.");
        m.put("mobile_number.digits", "Mobile number must be exactly 10 digits.");
        m.put("mobile_number.regex", "Enter a valid 10-digit Indian mobile number.");
        m.put("bene_id.required", "Either Bene ID or full beneficiary details must be provided.");
        m.put("bene_name.required", "Beneficiary name is required when adding a new beneficiary.");
        m.put("bene_pincode.required", "Beneficiary pincode is required when adding a new beneficiary.");
        m.put("bene_pincode.digits", "Beneficiary pincode must be exactly 6 digits.");
        v.messages(m);
        v.field("mobile_number").required().digits(10).regex("^[6-9][0-9]{9}$");
        boolean details = has(p, "bene_name") || has(p, "bene_account") || has(p, "bene_ifsc")
                || has(p, "bank_code") || has(p, "bene_pincode");
        if (p.containsKey("bene_id")) {
            v.field("bene_id").requiredIf(!details);
        }
        if (p.containsKey("bene_name")) {
            v.field("bene_name").requiredIf(has(p, "bene_account") || has(p, "bene_ifsc") || has(p, "bank_code")
                    || has(p, "bene_pincode")).string();
        }
        v.field("bank_name").required();
        v.field("bene_account").required();
        v.field("bank_code").required();
        if (p.containsKey("bene_pincode")) {
            v.field("bene_pincode").requiredIf(has(p, "bene_name") || has(p, "bene_account") || has(p, "bene_ifsc")
                    || has(p, "bank_code")).digits(6);
        }
        if (v.fails()) {
            return response.error("Validation Failed", v.errors(), HttpStatus.BAD_REQUEST);
        }
        User user = cu.user();
        if (!PayoutSupport.hasRole(user, "dd", "ret", "api")) {
            return response.error("Unauthorized action", List.of(), HttpStatus.FORBIDDEN);
        }
        PayoutSender sender = senderRepository.findFirstByMobileNumberAndPipe(s(p, "mobile_number"), TYPE).orElse(null);
        if (sender == null) {
            return response.error("Sender not found ", List.of(), HttpStatus.CONFLICT);
        }
        String bene = PayoutSupport.alnumName(s(p, "bene_name"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("RemitterNumber", s(p, "mobile_number"));
        payload.put("BeneName", bene);
        payload.put("BeneAccount", s(p, "bene_account"));
        payload.put("BeneIFSC", s(p, "ifsc_code"));
        payload.put("BankCode", s(p, "bank_code"));
        payload.put("BenePincode", s(p, "bene_pincode"));
        payload.put("ClientReq", uniqid("req_"));
        Map<String, Object> resp = providers.soliteck(SOL + "Payout/APIUserPayoutAddBeneToList", payload);
        if (resp == null || resp.isEmpty() || code(resp) == null) {
            return response.error("Invalid response from Soliteck API", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if ("200".equals(code(resp)) && resp.get("responseData") != null) {
            if (beneficiaryRepository.findFirstByMobileNumberAndAccountNumberAndIfscCodeAndType(
                    s(p, "mobile_number"), s(p, "bene_account"), s(p, "ifsc_code"), TYPE).isPresent()) {
                return response.error("Beneficiary already exists", List.of(), HttpStatus.CONFLICT);
            }
            PayoutBeneficiary b = new PayoutBeneficiary();
            b.setBeneficiaryName(bene);
            b.setIfscCode(s(p, "ifsc_code"));
            b.setAccountNumber(s(p, "bene_account"));
            b.setBankName(p.get("bank_name") != null ? s(p, "bank_name") : "");
            b.setType(TYPE);
            b.setSoliteckId(resp.get("responseData").toString());
            b.setSenderId(sender.getId());
            b.setMobileNumber(s(p, "mobile_number"));
            b.setIsVerified(p.get("is_verified") == null ? null : toInt(p.get("is_verified")));
            beneficiaryRepository.save(b);
        }
        return handleResponse(resp);
    }

    // POST payout/deleteBeneficiary2
    @PostMapping("payout/deleteBeneficiary2")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteBeneficiary2(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        Map<String, String> m = new java.util.HashMap<>();
        m.put("mobile_number.required", "Mobile number is required.");
        m.put("mobile_number.digits", "Mobile number must be exactly 10 digits.");
        m.put("mobile_number.regex", "Enter a valid 10-digit Indian mobile number.");
        m.put("bene_id.required", "Beneficiary ID is required.");
        m.put("bene_id.integer", "Beneficiary ID must be a valid number.");
        v.messages(m);
        v.field("mobile_number").required().string().digits(10).regex("^[6-9][0-9]{9}$");
        v.field("bene_id").required().integer();
        if (v.fails()) {
            return response.error("Validation Failed", v.errors(), HttpStatus.BAD_REQUEST);
        }
        if (!PayoutSupport.hasRole(cu.user(), "adm", "sadm", "ret", "api")) {
            return response.error("Unauthorized action", List.of(), HttpStatus.FORBIDDEN);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("RemitterNumber", s(p, "mobile_number"));
        payload.put("BeneId", s(p, "bene_id"));
        payload.put("ClientReq", uniqid("req"));
        Map<String, Object> resp = providers.soliteck(SOL + "Payout/APIUserPayoutDeleteBeneFromList", payload);
        if (resp == null || resp.isEmpty() || code(resp) == null) {
            return response.error("Invalid response from Soliteck API", List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if ("200".equals(code(resp))) {
            beneficiaryRepository.deleteByMobileAndSoliteckId(s(p, "mobile_number"), s(p, "bene_id"));
        }
        return handleResponse(resp);
    }

    // POST payout/verifyBeneficiary — the source returns the raw provider response
    @PostMapping("payout/verifyBeneficiary")
    public ResponseEntity<Map<String, Object>> verifyBeneficiary(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("mobile_number").required().digits(10);
        v.field("bene_id").required();
        v.field("bene_account").required();
        v.field("bene_ifsc").required();
        v.field("bank_code").required();
        v.field("client_order_id").required().numeric();
        if (v.fails()) {
            return response.error("Validation Failed", v.errors(), HttpStatus.BAD_REQUEST);
        }
        if (!PayoutSupport.hasRole(cu.user(), "adm", "sadm", "ret", "api")) {
            return response.error("Unauthorized action", List.of(), HttpStatus.FORBIDDEN);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("RemitterNumber", s(p, "mobile_number"));
        payload.put("BeneId", s(p, "bene_id"));
        payload.put("ClientOrderId", s(p, "client_order_id"));
        return ResponseEntity.ok(providers.soliteck(SOL + "Payout/PayoutVerifyBene", payload));
    }

    // POST payout/payOut2 (throttle:1,0.5)
    @PostMapping("payout/payOut2")
    @RateLimit(requests = 1, seconds = 30)
    public ResponseEntity<Map<String, Object>> payOut2(@AuthenticationPrincipal CurrentUser cu,
                                                       HttpServletRequest request) {
        User user = cu.user();
        PayoutCtx c = PayoutCtx.of(payload(), request);
        Validation v = rules.payOut2(c.p(), user);
        if (v.fails()) {
            return response.error("Validation failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        flow.consumeUuid(c, "payOut2");
        PayoutFlow.Spec spec = new PayoutFlow.Spec(
                false,
                mobile -> senderRepository.findFirstByMobileNumberAndPipe(mobile, TYPE).orElse(null),
                null,
                false,
                (txn, service, u, sender) -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("RemitterNumber", c.s("mobile_number"));
                    payload.put("BeneId", c.s("soliteck_id"));
                    payload.put("Amount", c.s("amount"));
                    payload.put("ClientOrderId", txn.getTxnId());
                    payload.put("Mode", c.s("mop"));
                    return providers.soliteck(SOL + "Payout/PayoutTransfer", payload);
                },
                handler::handleResponse);
        return flow.run(user, c, spec);
    }

    // POST payout/checkTransactionStatus
    @PostMapping("payout/checkTransactionStatus")
    public ResponseEntity<Map<String, Object>> checkTransactionStatus() {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("txn_id").required().string();
        if (v.fails()) {
            return support.validationAsMessage(v, 400);
        }
        Map<String, Object> resp = providers.soliteck(SOL + "AdminActivity/CheckTxnStatus", s(p, "txn_id"));
        if (code(resp) == null) {
            return response.error("Something went wrong", List.of(), HttpStatus.BAD_REQUEST);
        }
        List<Object> data = resp.get("responseData") == null ? null : Json.readList(resp.get("responseData").toString());
        String status = null;
        if (data != null && !data.isEmpty() && data.get(0) instanceof Map<?, ?> row && row.get("StatusId") != null) {
            status = switch (row.get("StatusId").toString()) {
                case "1" -> "Success";
                case "0" -> "Failed";
                case "3" -> "Pending";
                default -> null;
            };
        }
        return response.success(Objects.toString(resp.get("responseMessage"), null), status);
    }
}
