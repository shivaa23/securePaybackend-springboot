package com.securepay.modules.payout.service;

import com.securepay.common.web.Validation;
import com.securepay.modules.dmt.repository.VerificationTxnRepository;
import com.securepay.modules.payout.repository.PayoutSenderRepository;
import com.securepay.modules.payout.repository.PayoutTxnRepository;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.service.repository.ServiceRepository;
import com.securepay.modules.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Laravel-style validators of {@code PayoutValidation}, {@code SoliteckValidation},
 * {@code SenderValidation} and {@code BeneficiaryValidation} (rule order and messages preserved).
 */
@Component
public class PayoutRules {

    private static final String MOBILE = "^[6-9][0-9]{9}$";
    private static final String IFSC = "^[A-Z]{4}0[A-Z0-9]{6}$";

    private final ServiceRepository serviceRepository;
    private final PayoutTxnRepository payoutTxnRepository;
    private final VerificationTxnRepository verificationTxnRepository;
    private final PayoutSenderRepository senderRepository;

    public PayoutRules(ServiceRepository serviceRepository, PayoutTxnRepository payoutTxnRepository,
                       VerificationTxnRepository verificationTxnRepository,
                       PayoutSenderRepository senderRepository) {
        this.serviceRepository = serviceRepository;
        this.payoutTxnRepository = payoutTxnRepository;
        this.verificationTxnRepository = verificationTxnRepository;
        this.senderRepository = senderRepository;
    }

    /** {@code Rule::exists('services','id')->where(is_active=1 and is_active_api|is_active_users=1)}. */
    private boolean operatorActive(User user, String id) {
        try {
            ServiceEntity s = serviceRepository.findById(Long.valueOf(id.trim())).orElse(null);
            if (s == null || s.getIsActive() == null || s.getIsActive() != 1) {
                return false;
            }
            Integer flag = "api".equals(user.getRole()) ? s.getIsActiveApi() : s.getIsActiveUsers();
            return flag != null && flag == 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static Map<String, String> commonMessages(boolean amountMaxAs49000) {
        Map<String, String> m = new HashMap<>();
        m.put("client_ref.required", "client_ref is required.");
        m.put("client_ref.string", "The client_ref must be a string.");
        m.put("client_ref.unique", "The client_ref must be a unique.");
        m.put("client_ref.min", "The client_ref should be minimum 10 digits.");
        m.put("client_ref.max", "The client_ref should be maximum 20 digits.");
        m.put("beneficiary_name.required", "beneficiary_name is required.");
        m.put("beneficiary_name.string", "The beneficiary_name must be a string.");
        m.put("bank_name.required", "bank_name is required.");
        m.put("bank_name.string", "The bank_name must be a string.");
        m.put("bank_name.min", "The bank_name should be minimum 3 digits.");
        m.put("bank_name.max", "The bank_name should be maximum 50 digits.");
        m.put("bank_name.regex", "The bank_name must be numeric or alphanumeric string having 6 to 50 digits.");
        m.put("account_number.required", "account_number is required.");
        m.put("account_number.string", "account_number must be a string.");
        m.put("account_number.regex", "The account_number must be numeric or alphanumeric string having 3 to 50 digits.");
        m.put("ifsc_code.required", "ifsc_code is required.");
        m.put("ifsc_code.string", "ifsc_code must be a string.");
        m.put("ifsc_code.regex", "The ifsc_code must be a alphanumeric string having 11 digits.");
        m.put("mop.required", "mop is required.");
        m.put("mop.in", "mop must be one of: UPI for UPI Transfer else NEFT, IMPS, RTGS.");
        m.put("latitude.required", "Latitude is required.");
        m.put("latitude.numeric", "Latitude must be a number.");
        m.put("latitude.between", "Latitude must be between -90 and 90.");
        m.put("longitude.required", "Longitude is required.");
        m.put("longitude.numeric", "Longitude must be a number.");
        m.put("longitude.between", "Longitude must be between -180 and 180.");
        m.put("amount.required", "Amount is required.");
        m.put("amount.numeric", "Amount must be a valid number.");
        m.put("mobile_number.required", "Mobile number is required.");
        m.put("mobile_number.numeric", "The mobile number must be a number.");
        m.put("mobile_number.regex", "Please enter a valid 10-digit Indian mobile number.");
        m.put("operator.required", "Operator field is required.");
        m.put("operator.integer", "Operator must be a valid number.");
        m.put("operator.exists", "Selected operator does not exist or is inactive.");
        m.put("operator.in", "Selected operator is wrong.");
        return m;
    }

    /** {@code PayoutValidation::payOut}. */
    public Validation payOut(Map<String, Object> d, User user) {
        boolean upi = "12".equals(str(d.get("operator")));
        Validation v = Validation.of(d).messages(commonMessages(true));
        Map<String, String> extra = new HashMap<>();
        extra.put("amount.min", upi ? "Minimum amount allowed is 1000." : "Minimum amount allowed is 10.");
        extra.put("amount.max", upi ? "Maximum amount allowed is 25000." : "Minimum amount allowed is 49000.");
        v.messages(extra);
        v.field("client_ref").required().unique(payoutTxnRepository::existsByClientRef).string().min(10).max(20);
        v.field("beneficiary_name").required().string().min(3).max(255);
        v.field("purpose").sometimes().string();
        if (upi) {
            v.field("bank_name").nullable().string().min(3).max(50).regex("^[a-zA-Z\\s]+$");
            v.field("account_number").required().string().regex("^[A-Za-z0-9._-]{2,50}@[A-Za-z0-9]{2,64}$");
            v.field("ifsc_code").nullable().string().regex(IFSC);
            v.field("mop").required().string().in("UPI");
            v.field("amount").required().numeric().min(1000).max(25000);
        } else {
            v.field("bank_name").required().string().min(3).max(60);
            v.field("account_number").required().string().regex("^[A-Za-z0-9]{6,20}$");
            v.field("ifsc_code").required().string().regex(IFSC);
            v.field("mop").required().string().in("NEFT", "IMPS", "RTGS");
            v.field("amount").required().numeric().min(100).max(49000);
        }
        v.field("latitude").required().numeric().between(-90, 90);
        v.field("longitude").required().numeric().between(-180, 180);
        v.field("mobile_number").required().numeric().regex(MOBILE);
        v.field("operator").required().integer().in("11", "12")
                .exists(id -> operatorActive(user, id));
        return v;
    }

    /** {@code SoliteckValidation::payOut2} (operator 81). */
    public Validation payOut2(Map<String, Object> d, User user) {
        return switchPayout(d, user, "81", true);
    }

    /** {@code SoliteckValidation::payOutAbhayam} (operators 336/337; reused by payOutUtkal). */
    public Validation payOutAbhayam(Map<String, Object> d, User user) {
        return switchPayout(d, user, "336,337", false);
    }

    private Validation switchPayout(Map<String, Object> d, User user, String allowed, boolean soliteck) {
        boolean upi = "12".equals(str(d.get("operator")));
        Validation v = Validation.of(d).messages(commonMessages(false));
        Map<String, String> extra = new HashMap<>();
        extra.put("amount.min", "Minimum amount allowed is 10.");
        extra.put("amount.max", "Maximum amount allowed is 49000.");
        v.messages(extra);
        v.field("client_ref").required().unique(payoutTxnRepository::existsByClientRef).string().min(10).max(20);
        v.field("beneficiary_name").required().string().min(3).max(255);
        v.field("purpose").sometimes().string();
        v.field("operator").required().integer().in(allowed.split(","))
                .exists(id -> operatorActive(user, id));
        if (upi) {
            v.field("bank_name").nullable().string().min(3).max(50);
            v.field("account_number").required().string().regex("^[a-zA-Z0-9.\\-_]{2,50}@[a-zA-Z]{3,64}$");
            v.field("ifsc_code").nullable().string().regex(IFSC);
            v.field("mop").required().string().in("UPI");
        } else {
            v.field("bank_name").required().string().min(3).max(50);
            v.field("account_number").required().string().regex("^[A-Z0-9]{6,20}$");
            v.field("ifsc_code").required().string().regex(IFSC);
            v.field("mop").required().string().in("NEFT", "IMPS", "RTGS");
        }
        v.field("latitude").required().numeric().between(-90, 90);
        v.field("longitude").required().numeric().between(-180, 180);
        v.field("amount").required().numeric().min(10).max(50000);
        v.field("mobile_number").required().numeric().regex(MOBILE);
        v.field(soliteck ? "soliteck_id" : "bene_id").required();
        return v;
    }

    /** {@code SoliteckValidation::upiUtkal} (operator 338). */
    public Validation upiUtkal(Map<String, Object> d, User user) {
        Validation v = Validation.of(d);
        Map<String, String> m = new HashMap<>();
        m.put("client_ref.required", "client_ref is required.");
        m.put("client_ref.unique", "client_ref must be unique.");
        m.put("latitude.required", "Latitude is required.");
        m.put("longitude.required", "Longitude is required.");
        m.put("amount.required", "Amount is required.");
        m.put("amount.min", "Minimum amount is ₹100.");
        m.put("amount.max", "Maximum amount is ₹50000.");
        m.put("mop.required", "Mode of payment is required.");
        m.put("mop.in", "mop must be UPI only.");
        m.put("email.email", "Enter a valid email address.");
        m.put("operator.required", "Operator is required.");
        m.put("operator.in", "Invalid operator.");
        m.put("operator.exists", "Operator does not exist or inactive.");
        m.put("mpin.required", "mpin is required.");
        v.messages(m);
        v.field("client_ref").required().unique(payoutTxnRepository::existsByClientRef).string().min(10).max(20);
        v.field("account_number").required().string()
                .regex("^([a-zA-Z0-9.\\-_]{2,50}|[6-9]\\d{9})@[a-zA-Z0-9]{3,64}$");
        v.field("beneficiary_name").required().string().min(3).max(255);
        v.field("mobile_number").required().numeric().regex(MOBILE);
        v.field("latitude").required().numeric().between(-90, 90);
        v.field("longitude").required().numeric().between(-180, 180);
        v.field("amount").required().numeric().min(100).max(50000);
        v.field("mop").required().string().in("UPI");
        v.field("email").nullable().email().max(150);
        v.field("address").nullable().string().max(255);
        v.field("purpose").sometimes().string();
        v.field("operator").required().integer().in("338").exists(id -> operatorActive(user, id));
        v.field("mpin").required().string().min(4).max(8);
        return v;
    }

    /** {@code SoliteckValidation::upiVerification} (operator 21). */
    public Validation upiVerification(Map<String, Object> d, User user) {
        Validation v = Validation.of(d);
        v.field("mobile_number").required().digits(10).numeric();
        v.field("account_number").required();
        v.field("latitude").required();
        v.field("longitude").required();
        v.field("beneficiary_name").required();
        v.field("client_ref").required().string().unique(verificationTxnRepository::existsByClientRef);
        v.field("mpin").required().numeric().digits(6);
        v.field("operator").required().integer().in("21").exists(id -> operatorActive(user, id));
        return v;
    }

    /** {@code SenderValidation::registerSender}. */
    public Validation registerSender(Map<String, Object> d) {
        Validation v = Validation.of(d);
        Map<String, String> m = new HashMap<>();
        m.put("sender_name.required", "Sender name is required.");
        m.put("sender_name.string", "Sender name must be a string.");
        m.put("sender_name.min", "Sender name should be at least 3 characters.");
        m.put("sender_name.max", "Sender name should not exceed 50 characters.");
        m.put("sender_name.regex", "Sender name should only contain letters and spaces.");
        m.put("mobile_number.required", "Mobile number is required.");
        m.put("mobile_number.numeric", "Mobile number must be numeric.");
        m.put("mobile_number.regex", "Enter a valid 10-digit Indian mobile number.");
        m.put("mobile_number.unique", "This mobile number is already registered.");
        v.messages(m);
        v.field("sender_name").required().string().min(3).max(50).regex("^[a-zA-Z\\s]+$");
        v.field("mobile_number").required().numeric().regex(MOBILE)
                .unique(senderRepository::existsByMobileNumber);
        return v;
    }

    /** {@code SenderValidation::verifySender}. */
    public Validation verifySender(Map<String, Object> d) {
        Validation v = Validation.of(d);
        Map<String, String> m = new HashMap<>();
        m.put("otp.required", "OTP is required.");
        m.put("otp.digits", "OTP must be exactly 6 digits.");
        m.put("otp_ref.required", "OTP reference is required.");
        m.put("otp_ref.integer", "OTP reference must be a string.");
        m.put("otp_ref.min", "OTP reference must be at least 6 characters.");
        v.messages(m);
        v.field("mobile_number").required().integer().exists(senderRepository::existsByMobileNumber);
        v.field("otp").required().digits(6);
        v.field("otp_ref").required().integer().min(6);
        return v;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
