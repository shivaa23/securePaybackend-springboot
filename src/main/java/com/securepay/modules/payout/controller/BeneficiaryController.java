package com.securepay.modules.payout.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Validation;
import com.securepay.modules.dmt.entity.DmtBank;
import com.securepay.modules.dmt.repository.DmtBankRepository;
import com.securepay.modules.payout.entity.PayoutBeneficiary;
import com.securepay.modules.payout.repository.PayoutBeneficiaryRepository;
import com.securepay.modules.payout.repository.PayoutSenderRepository;
import com.securepay.modules.payout.service.PayoutSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.securepay.modules.payout.controller.SenderController.field;
import static com.securepay.modules.payout.controller.SenderController.orderedMap;

/** Port of {@code Payout/Controllers/BeneficiaryController}. */
@RestController
public class BeneficiaryController extends BaseController {

    private static final String IFSC = "^[A-Z]{4}0[A-Z0-9]{6}$";

    private final ResponseFactory response;
    private final PayoutSupport support;
    private final PayoutBeneficiaryRepository beneficiaryRepository;
    private final PayoutSenderRepository senderRepository;
    private final DmtBankRepository dmtBankRepository;

    public BeneficiaryController(ResponseFactory response, PayoutSupport support,
                                 PayoutBeneficiaryRepository beneficiaryRepository,
                                 PayoutSenderRepository senderRepository, DmtBankRepository dmtBankRepository) {
        this.response = response;
        this.support = support;
        this.beneficiaryRepository = beneficiaryRepository;
        this.senderRepository = senderRepository;
        this.dmtBankRepository = dmtBankRepository;
    }

    // POST payout/getBeneficiaries — every row, newest first (as the source does)
    @PostMapping("payout/getBeneficiaries")
    public ResponseEntity<Map<String, Object>> getBeneficiaries() {
        return response.success("Beneficiaries fetched successfully", beneficiaryRepository.findAllByOrderByIdDesc());
    }

    // POST payout/getBeneficiarySchema
    @PostMapping("payout/getBeneficiarySchema")
    public ResponseEntity<Map<String, Object>> getBeneficiarySchema() {
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> banks = new ArrayList<>();
        for (DmtBank b : dmtBankRepository.findByPipeOrderByNameAsc("INSTANTPAY")) {
            if (seen.add(b.getName())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("bank_name", b.getName());
                m.put("bank_id", b.getBankId());
                m.put("ifsc", b.getIfsc());
                banks.add(m);
            }
        }
        Map<String, Object> bank = field("bank_id", "Bank", "autocomplete", "Select a bank", true,
                orderedMap("type", "string"));
        bank.put("options", banks);
        // PHP key order: name,label,type,placeholder,options,required,validation
        Map<String, Object> bankOrdered = new LinkedHashMap<>();
        bankOrdered.put("name", bank.get("name"));
        bankOrdered.put("label", bank.get("label"));
        bankOrdered.put("type", bank.get("type"));
        bankOrdered.put("placeholder", bank.get("placeholder"));
        bankOrdered.put("options", banks);
        bankOrdered.put("required", true);
        bankOrdered.put("validation", bank.get("validation"));
        List<Object> fields = List.of(
                bankOrdered,
                field("ifsc_code", "IFSC Code", "text", "Enter IFSC code", true,
                        orderedMap("type", "string", "regex", "/^[A-Z]{4}0[A-Z0-9]{6}$/")),
                field("beneficiary_name", "Beneficiary Name", "text", "Enter beneficiary name", true,
                        orderedMap("type", "string")),
                field("account_number", "Account Number", "text", "Enter account number", true,
                        orderedMap("type", "string")));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("formName", "Add Beneficiary");
        schema.put("fields", fields);
        return response.success("Beneficiaries  schema fetched successfully", schema);
    }

    // POST payout/getBeneficiarySchemaUpi
    @PostMapping("payout/getBeneficiarySchemaUpi")
    public ResponseEntity<Map<String, Object>> getBeneficiarySchemaUpi() {
        String[][] opts = {
                {"Paytm", "paytm"}, {"PhonePe", "ybl"}, {"Google Pay (okhdfcbank)", "okhdfcbank"},
                {"Google Pay (okicici)", "okicici"}, {"Google Pay (oksbi)", "oksbi"}, {"Amazon Pay", "apl"},
                {"BHIM", "upi"}, {"ICICI Bank", "icici"}, {"HDFC Bank", "hdfcbank"}, {"Axis Bank", "axisbank"},
                {"SBI (State Bank of India)", "sbi"}, {"Kotak Mahindra Bank", "kotak"},
                {"Punjab National Bank (PNB)", "pnb"}, {"Bank of Baroda", "barodampay"},
                {"Yes Bank", "yesbank"}, {"Other", "Other"}};
        List<Object> options = new ArrayList<>();
        for (String[] o : opts) {
            options.add(orderedMap("label", o[0], "value", o[1]));
        }
        Map<String, Object> suffix = new LinkedHashMap<>();
        suffix.put("name", "suffix");
        suffix.put("label", "Suffix");
        suffix.put("type", "dropdown");
        suffix.put("placeholder", "Select payment method");
        suffix.put("required", true);
        suffix.put("options", options);
        List<Object> fields = List.of(
                field("beneficiary_name", "Beneficiary Name", "text", "Enter beneficiary name", true,
                        orderedMap("type", "string", "minLength", 3, "maxLength", 50)),
                field("prefix", "Prefix", "text", "Prefix", true, orderedMap("type", "string")),
                suffix);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("formName", "Beneficiary Schema");
        schema.put("fields", fields);
        return response.success("Beneficiaries  schema fetched successfully", schema);
    }

    /** {@code BeneficiaryValidation::createBeneficiary}. */
    Validation createRules(Map<String, Object> d) {
        boolean notUpi = !"UPI".equals(Objects.toString(d.get("type"), null));
        Validation v = Validation.of(d);
        Map<String, String> m = new java.util.HashMap<>();
        m.put("ifsc_code.required", "IFSC code is required unless type is UPI.");
        m.put("type.required", "Type is required.");
        m.put("type.in", "Type must be either UPI or BANK.");
        v.messages(m);
        v.field("sender_id").required().integer().exists(id -> senderRepository.existsById(Long.valueOf(id.trim())));
        v.field("beneficiary_name").required().string().min(3).max(50);
        v.field("account_number").required().string();
        v.field("ifsc_code").requiredIf(notUpi).string().regex(IFSC);
        v.field("bank_name").requiredIf(notUpi).string().min(3).max(100);
        v.field("mobile_number").sometimes().numeric().regex("^[6-9][0-9]{9}$");
        v.field("type").required().in("UPI", "BANK");
        return v;
    }

    ResponseEntity<Map<String, Object>> create(String forcedType) {
        Map<String, Object> p = payload();
        Validation v = createRules(p);
        if (v.fails()) {
            return response.error("Validation error", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        String name = Objects.toString(p.get("beneficiary_name"), "")
                .replaceAll("[^A-Za-z\\s]", " ").replaceAll("\\s+", " ");
        PayoutBeneficiary b = new PayoutBeneficiary();
        b.setSenderId(Long.valueOf(p.get("sender_id").toString().trim()));
        b.setBeneficiaryName(PayoutSupport.ucwords(name.trim()));
        b.setAccountNumber(Objects.toString(p.get("account_number"), null));
        b.setIfscCode(Objects.toString(p.get("ifsc_code"), null));
        b.setType(forcedType != null ? forcedType : Objects.toString(p.get("type"), null));
        b.setBankName(Objects.toString(p.get("bank_name"), null));
        b.setMobileNumber(Objects.toString(p.get("mobile_number"), null));
        b.setIsVerified(p.get("is_verified") == null ? 0 : toInt(p.get("is_verified")));
        b = beneficiaryRepository.save(b);
        return response.success("Beneficiary created successfully", b);
    }

    static int toInt(Object o) {
        String s = o.toString();
        if ("true".equalsIgnoreCase(s)) {
            return 1;
        }
        if ("false".equalsIgnoreCase(s)) {
            return 0;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // POST payout/createBeneficiary
    @PostMapping("payout/createBeneficiary")
    public ResponseEntity<Map<String, Object>> createBeneficiary() {
        return create(null);
    }

    // POST payout/deleteBeneficiary
    @PostMapping("payout/deleteBeneficiary")
    public ResponseEntity<Map<String, Object>> deleteBeneficiary() {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        Map<String, String> m = new java.util.HashMap<>();
        m.put("id.required", "Beneficiary ID is required.");
        m.put("id.integer", "Beneficiary ID must be a valid number.");
        m.put("id.exists", "Beneficiary does not exist.");
        v.messages(m);
        v.field("id").required().integer().exists(id -> beneficiaryRepository.existsById(Long.valueOf(id.trim())));
        if (v.fails()) {
            return response.error("Validation failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Long senderId = p.get("sender_id") == null ? null : Long.valueOf(p.get("sender_id").toString().trim());
        PayoutBeneficiary b = senderId == null ? null
                : beneficiaryRepository.findFirstBySenderIdAndId(senderId, Long.valueOf(p.get("id").toString().trim()))
                .orElse(null);
        if (b == null) {
            return response.error("Beneficiary not found", List.of(), HttpStatus.NOT_FOUND);
        }
        beneficiaryRepository.delete(b);
        return response.success("Beneficiary deleted successfully", List.of());
    }
}
