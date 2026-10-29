package com.securepay.modules.payout.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Paginated;
import com.securepay.common.web.Validation;
import com.securepay.modules.payout.entity.PayoutBeneficiary;
import com.securepay.modules.payout.entity.PayoutSender;
import com.securepay.modules.payout.repository.PayoutBeneficiaryRepository;
import com.securepay.modules.payout.repository.PayoutSenderRepository;
import com.securepay.modules.payout.service.PayoutCtx;
import com.securepay.modules.payout.service.PayoutRules;
import com.securepay.modules.payout.service.PayoutSupport;
import com.securepay.modules.auth.service.OtpService;
import com.securepay.modules.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.securepay.modules.payout.service.PayoutSupport.SENDER_ROLES;

/** Port of {@code Payout/Controllers/SenderController} (ImpsGuru-style senders, pipe = null). */
@RestController
public class SenderController extends BaseController {

    private final ResponseFactory response;
    private final PayoutRules rules;
    private final PayoutSupport support;
    private final PayoutSenderRepository senderRepository;
    private final PayoutBeneficiaryRepository beneficiaryRepository;
    private final OtpService otpService;

    public SenderController(ResponseFactory response, PayoutRules rules, PayoutSupport support,
                            PayoutSenderRepository senderRepository,
                            PayoutBeneficiaryRepository beneficiaryRepository, OtpService otpService) {
        this.response = response;
        this.rules = rules;
        this.support = support;
        this.senderRepository = senderRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.otpService = otpService;
    }

    private ResponseEntity<Map<String, Object>> unauthorised() {
        return response.error("Unauthorised Action", "", HttpStatus.FORBIDDEN);
    }

    // GET payout/updateSenderLimit — only from the whitelisted cron IP
    @GetMapping("payout/updateSenderLimit")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateSenderLimit(HttpServletRequest request) {
        PayoutCtx c = PayoutCtx.of(payload(), request);
        if (!"103.49.124.232".equals(c.ip())) {
            return response.error("Unauthorised Action", "", HttpStatus.FORBIDDEN);
        }
        senderRepository.setAllRemLimit(new BigDecimal("200000"));
        return response.success("Sender updated", "");
    }

    // POST payout/registerSender
    @PostMapping("payout/registerSender")
    public ResponseEntity<Map<String, Object>> registerSender(@AuthenticationPrincipal CurrentUser cu) {
        Validation v = rules.registerSender(payload());
        if (v.fails()) {
            return response.jsonResponse("1000", "Failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        User user = cu.user();
        if (!PayoutSupport.hasRole(user, "ret", "dd", "api")) {
            return unauthorised();
        }
        Map<String, Object> p = payload();
        PayoutSender sender = new PayoutSender();
        sender.setUserId(user.getId());
        sender.setSenderName(Objects.toString(p.get("sender_name"), null));
        sender.setEmail(Objects.toString(p.get("email"), null));
        sender.setMobileNumber(Objects.toString(p.get("mobile_number"), null));
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

    // POST payout/verifySender
    @PostMapping("payout/verifySender")
    public ResponseEntity<Map<String, Object>> verifySender(@AuthenticationPrincipal CurrentUser cu) {
        Validation v = rules.verifySender(payload());
        if (v.fails()) {
            return support.validationFailed(v, 422);
        }
        User user = cu.user();
        if (!PayoutSupport.hasRole(user, "ret", "dd", "api")) {
            return unauthorised();
        }
        Map<String, Object> p = payload();
        PayoutSender sender = senderRepository.findFirstByMobileNumber(Objects.toString(p.get("mobile_number")))
                .orElse(null);
        if (sender == null) {
            return response.error("Sender not found", "", HttpStatus.NOT_FOUND);
        }
        OtpService.Result r = otpService.verifyOtp(user, Objects.toString(p.get("otp_ref")),
                Objects.toString(p.get("otp")));
        if (!r.success()) {
            return response.error(r.message(), null, HttpStatus.FORBIDDEN);
        }
        sender.setIsActive(1);
        sender.setKycStatus("verified");
        sender = senderRepository.save(sender);
        return response.success("Sender Verified Successfully", sender);
    }

    // POST payout/getSender
    @PostMapping("payout/getSender")
    public ResponseEntity<Map<String, Object>> getSender(@AuthenticationPrincipal CurrentUser cu) {
        User user = cu.user();
        if (!PayoutSupport.hasRole(user, "ret", "dd", "api")) {
            return unauthorised();
        }
        Map<String, Object> p = payload();
        String mobile = Objects.toString(p.get("mobile_number"), null);
        if (mobile == null || mobile.isBlank()) {
            return response.error("Mobile number required", "", HttpStatus.FORBIDDEN);
        }
        String type = p.get("type") == null ? "BANK" : p.get("type").toString().toUpperCase();
        PayoutSender sender = senderRepository.findFirstByMobileNumber(mobile).orElse(null);
        if (sender == null) {
            return response.success("Sender Not Found", "");
        }
        if ("UPI".equals(type)) {
            support.withBeneficiaries(sender, "UPI");
        } else {
            support.withBeneficiariesNot(sender, "UPI");
        }
        return senderStateResponse(user, sender);
    }

    /** Shared tail of getSender / getSender3: inactive → mail OTP + 403, pending KYC, else the sender. */
    ResponseEntity<Map<String, Object>> senderStateResponse(User user, PayoutSender sender) {
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

    // POST payout/getSenderByAcc
    @PostMapping("payout/getSenderByAcc")
    public ResponseEntity<Map<String, Object>> getSenderByAcc(@AuthenticationPrincipal CurrentUser cu) {
        User user = cu.user();
        if (!PayoutSupport.hasRole(user, "ret", "dd", "api")) {
            return unauthorised();
        }
        Map<String, Object> p = payload();
        String acc = Objects.toString(p.get("account_number"), null);
        if (acc == null || acc.isBlank()) {
            return response.error("Account number required", "", HttpStatus.FORBIDDEN);
        }
        String type = p.get("type") == null ? "BANK" : p.get("type").toString().toUpperCase();
        List<String> mobiles = new ArrayList<>();
        for (PayoutBeneficiary b : beneficiaryRepository.findByAccountNumberAndType(acc, type)) {
            if (b.getMobileNumber() != null && !b.getMobileNumber().isBlank()) {
                mobiles.add(b.getMobileNumber());
            }
        }
        if (mobiles.isEmpty()) {
            return response.error("Account Not Found", "", HttpStatus.NOT_FOUND);
        }
        return response.success("Account numbers fetched successfully", mobiles);
    }

    // POST payout/getSenders
    @PostMapping("payout/getSenders")
    public ResponseEntity<Map<String, Object>> getSenders(@AuthenticationPrincipal CurrentUser cu) {
        if (!PayoutSupport.hasRole(cu.user(), "sadm", "adm")) {
            return unauthorised();
        }
        int page = 1;
        try {
            page = Math.max(1, Integer.parseInt(Objects.toString(payload().get("page"), "1")));
        } catch (NumberFormatException ignored) {
            // default page 1
        }
        var result = senderRepository.findAll(PageRequest.of(page - 1, 10));
        return response.success("payout Txns fetched", Paginated.of(result));
    }

    // POST payout/verifySenderSchema
    @PostMapping("payout/verifySenderSchema")
    public ResponseEntity<Map<String, Object>> verifySenderSchema() {
        Map<String, Object> otp = field("otp", "OTP", "number", "Enter 6-digit OTP", true,
                Map.of("type", "digits", "length", 6));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("formName", "Verify Sender");
        schema.put("fields", List.of(otp));
        return response.success("Schema fetched", schema);
    }

    // POST payout/registerSenderSchema
    @PostMapping("payout/registerSenderSchema")
    public ResponseEntity<Map<String, Object>> registerSenderSchema() {
        Map<String, Object> name = field("sender_name", "Sender Name", "text", "Enter sender name", true,
                orderedMap("type", "string", "min", 3, "max", 50, "regex", "^[a-zA-Z\\s]+$"));
        Map<String, Object> mobile = field("mobile_number", "Mobile Number", "tel",
                "Enter 10-digit mobile number", true,
                orderedMap("type", "string", "regex", "^[6-9][0-9]{9}$", "unique", "payout_senders,mobile_number"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("formName", "Register Sender");
        schema.put("fields", List.of(name, mobile));
        return response.success("Schema fetched", schema);
    }

    static Map<String, Object> field(String name, String label, String type, String placeholder,
                                     boolean required, Map<String, Object> validation) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("label", label);
        f.put("type", type);
        f.put("placeholder", placeholder);
        f.put("required", required);
        f.put("validation", validation);
        return f;
    }

    static Map<String, Object> orderedMap(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i].toString(), kv[i + 1]);
        }
        return m;
    }
}
