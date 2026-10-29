package com.securepay.modules.payout.service;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.Validation;
import com.securepay.modules.auth.service.OtpService;
import com.securepay.modules.message.service.SmsService;
import com.securepay.modules.payout.entity.PayoutSender;
import com.securepay.modules.payout.repository.PayoutBeneficiaryRepository;
import com.securepay.modules.template.entity.Template;
import com.securepay.modules.template.repository.TemplateRepository;
import com.securepay.modules.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Helpers shared by the payout controllers. */
@Component
public class PayoutSupport {

    public static final List<String> SENDER_ROLES = List.of("ret", "dd", "api");

    private final PayoutBeneficiaryRepository beneficiaryRepository;
    private final TemplateRepository templateRepository;
    private final OtpService otpService;
    private final SmsService smsService;
    private final ResponseFactory response;

    public PayoutSupport(PayoutBeneficiaryRepository beneficiaryRepository,
                         TemplateRepository templateRepository, OtpService otpService,
                         SmsService smsService, ResponseFactory response) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.templateRepository = templateRepository;
        this.otpService = otpService;
        this.smsService = smsService;
        this.response = response;
    }

    public static boolean hasRole(User user, String... roles) {
        return Arrays.asList(roles).contains(user.getRole());
    }

    /** Eloquent {@code with(['beneficiary' => where type = X])}. */
    public PayoutSender withBeneficiaries(PayoutSender s, String type) {
        s.setBeneficiary(beneficiaryRepository.findByMobileNumberAndTypeOrderByIdAsc(s.getMobileNumber(), type));
        return s;
    }

    /** Eloquent {@code with(['beneficiary' => where type != X])}. */
    public PayoutSender withBeneficiariesNot(PayoutSender s, String type) {
        s.setBeneficiary(beneficiaryRepository.findByMobileNumberAndTypeNotOrderByIdAsc(s.getMobileNumber(), type));
        return s;
    }

    /**
     * Issues a login OTP and mails it using the {@code MOB_VERIFY} template (SMS replaced by email).
     *
     * @return the otp_ref, or empty when the template is not configured
     */
    public Optional<String> sendMobVerifyOtp(User user, String senderMobile) {
        Template template = templateRepository.findFirstByName("MOB_VERIFY").orElse(null);
        if (template == null) {
            return Optional.empty();
        }
        Map<String, String> otp = otpService.loginOtp(user.getId());
        String message = template.getMessage() == null ? "" : template.getMessage().replace("{$otp}", otp.get("otp"));
        smsService.send(senderMobile, message, template.getTempId(), user);
        return Optional.of(otp.get("otp_ref"));
    }

    public ResponseEntity<Map<String, Object>> validationFailed(Validation v, int code) {
        return response.error("Validation failed", v.errors(), HttpStatus.valueOf(code));
    }

    /** {@code ResponseHelper::error($validator->errors(), [], $code)} (MessageBag as the message). */
    public ResponseEntity<Map<String, Object>> validationAsMessage(Validation v, int code) {
        return response.errorWithObjectMessage(v.errors(), List.of(), HttpStatus.valueOf(code));
    }

    /** Laravel ucwords(): upper-case the first letter of every space-separated word. */
    public static String ucwords(String s) {
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (char ch : s.toCharArray()) {
            sb.append(up ? Character.toUpperCase(ch) : ch);
            up = ch == ' ';
        }
        return sb.toString();
    }

    /** {@code preg_replace('/[^a-zA-Z\s]/','', preg_replace('/\s+/',' ', trim($v)))}. */
    public static String lettersOnly(String v) {
        String t = v == null ? "" : v.trim().replaceAll("\\s+", " ");
        return t.replaceAll("[^a-zA-Z\\s]", "");
    }

    /** Beneficiary name sanitiser used by the Abhayam/Utkal/Soliteck add flows. */
    public static String alnumName(String v) {
        String t = v == null ? "" : v.replaceAll("[^A-Za-z0-9 ]", "");
        return t.replaceAll("\\s+", " ").trim();
    }
}
