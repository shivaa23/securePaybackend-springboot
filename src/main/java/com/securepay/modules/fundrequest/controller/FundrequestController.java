package com.securepay.modules.fundrequest.controller;

import com.securepay.common.audit.AuditLogService;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.security.PasswordService;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Paginated;
import com.securepay.modules.account.repository.AccountRepository;
import com.securepay.modules.bank.entity.Bank;
import com.securepay.modules.bank.repository.BankRepository;
import com.securepay.modules.fundrequest.entity.FundRequest;
import com.securepay.modules.fundrequest.entity.FundTxnId;
import com.securepay.modules.fundrequest.repository.FundRequestRepository;
import com.securepay.modules.fundrequest.repository.FundTxnIdRepository;
import com.securepay.modules.fundrequest.service.FundRequestService;
import com.securepay.modules.fundrequest.service.FundRequestService.FundRequestException;
import com.securepay.modules.fundrequest.service.ReceiptStorage;
import com.securepay.modules.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FundrequestController — full port of app/Modules/FundRequest/Controllers/FundRequestController.
 * Endpoints: getTxnId, getFundRequestsSchema, createFundRequest, createFundRequestnew,
 * getFundRequests, updateFundRequest (approve/reject), editFundRequest, deleteFundRequest,
 * pendingCount, and the secure receipt image route.
 */
@RestController
public class FundrequestController extends BaseController {

    private static final Set<String> ADMIN_ROLES = Set.of("adm", "sadm");
    /** Roles that may raise a fund request (create). */
    private static final Set<String> CREATOR_ROLES = Set.of("ret", "dd", "asm", "di", "md");
    /** Roles that may edit their own pending request. */
    private static final Set<String> EDITOR_ROLES = Set.of("ret", "dd", "md", "di");
    private static final List<String> MODES = List.of(
            "RTGS", "NEFT", "IMPS", "UPI", "FT", "Cash Deposit Branch",
            "Cash Deposit CDM", "Cash At Office", "CREDIT");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final ResponseFactory response;
    private final FundRequestRepository fundRequestRepository;
    private final FundTxnIdRepository fundTxnIdRepository;
    private final AccountRepository accountRepository;
    private final BankRepository bankRepository;
    private final FundRequestService fundRequestService;
    private final ReceiptStorage receiptStorage;
    private final AuditLogService auditLogService;
    private final PasswordService passwordService;

    public FundrequestController(ResponseFactory response,
                                 FundRequestRepository fundRequestRepository,
                                 FundTxnIdRepository fundTxnIdRepository,
                                 AccountRepository accountRepository,
                                 BankRepository bankRepository,
                                 FundRequestService fundRequestService,
                                 ReceiptStorage receiptStorage,
                                 AuditLogService auditLogService,
                                 PasswordService passwordService) {
        this.response = response;
        this.fundRequestRepository = fundRequestRepository;
        this.fundTxnIdRepository = fundTxnIdRepository;
        this.accountRepository = accountRepository;
        this.bankRepository = bankRepository;
        this.fundRequestService = fundRequestService;
        this.receiptStorage = receiptStorage;
        this.auditLogService = auditLogService;
        this.passwordService = passwordService;
    }

    // ------------------------------------------------------------------
    // Secure receipt image
    // ------------------------------------------------------------------

    // GET /secure-images/{filename} (legacy root path used in receipt URLs) and
    // GET fundrequest/secure-images/{filename} — serves jpg/png receipts only. Public so that
    // <img src> in the browser can load them (the PHP route has no auth middleware either).
    @GetMapping({"secure-images/{filename}", "fundrequest/secure-images/{filename}"})
    public ResponseEntity<Resource> secureImage(@PathVariable String filename) throws Exception {
        Path path = receiptStorage.resolve(filename);
        if (path == null) {
            return ResponseEntity.notFound().build();
        }
        String mime = receiptStorage.mimeOf(path);
        if (mime == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime))
                .body(new FileSystemResource(path));
    }

    // ------------------------------------------------------------------
    // Read endpoints
    // ------------------------------------------------------------------

    // POST fundrequest/getTxnId — issue and persist a transaction id for the caller.
    @PostMapping("fundrequest/getTxnId")
    public ResponseEntity<Map<String, Object>> fundrequestGetTxnId(
            @AuthenticationPrincipal CurrentUser currentUser) {
        User user = userOf(currentUser);
        String txn = "TXN" + (System.currentTimeMillis() / 1000)
                + (1000 + (int) (Math.random() * 9000));
        FundTxnId rec = new FundTxnId();
        rec.setUserId(user == null ? null : user.getId());
        rec.setTxnId(txn);
        fundTxnIdRepository.save(rec);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("txn_id", rec.getTxnId());
        data.put("user_id", rec.getUserId());
        return response.success("Transaction ID generated successfully", data);
    }

    // POST fundrequest/getFundRequestsSchema — form schema (banks + payment modes).
    @PostMapping("fundrequest/getFundRequestsSchema")
    public ResponseEntity<Map<String, Object>> fundrequestGetFundRequestsSchema() {
        List<Map<String, Object>> banks = bankRepository.findByStatus(1).stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("name", b.getName());
            m.put("account_number", b.getAccountNumber());
            m.put("ifsc", b.getIfsc());
            return m;
        }).collect(Collectors.toList());

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("bank_name", "Bank Name", "dropdown", "Enter bank name", true, banks));
        fields.add(field("mode", "Mode", "dropdown", "Select mode", true, MODES));
        fields.add(field("bank_ref_id", "Bank Reference ID ", "text", "Enter reference id", true, null));
        fields.add(field("date", "Transaction Date", "number", "Enter date as timestamp", true, null));
        fields.add(field("amount", "Amount", "number", "Enter amount", true, null));
        fields.add(field("remark", "Remark", "textarea", "Enter remark", true, null));
        fields.add(field("receipt", "Upload receipt", "file", null, false, null));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("formName", "Fund Request");
        schema.put("fields", fields);
        return response.success("FundRequests fetched", schema);
    }

    /**
     * POST fundrequest/getFundRequests — non-admins see only their own requests. Filters: status
     * (ignored when blank/"all"), user_id, from_date+to_date. id desc; paginated unless export=1.
     * Receipt paths are rewritten to the secure-images URL. Export for admins is trimmed to the
     * report columns (approved filter or admin role).
     */
    @PostMapping("fundrequest/getFundRequests")
    public ResponseEntity<Map<String, Object>> fundrequestGetFundRequests(
            @AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest http) {
        User authUser = userOf(currentUser);
        boolean isAdmin = authUser != null && ADMIN_ROLES.contains(String.valueOf(authUser.getRole()));

        String status = payloadString("status");
        String userId = payloadString("user_id");
        String fromDate = payloadString("from_date");
        String toDate = payloadString("to_date");
        String remarkFilter = payloadString("remark");
        String adminRemarkFilter = payloadString("admin_remark");
        boolean export = "1".equals(payloadString("export"));
        String baseUrl = http.getScheme() + "://" + http.getServerName()
                + (http.getServerPort() == 80 || http.getServerPort() == 443 ? "" : ":" + http.getServerPort());

        Specification<FundRequest> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (!isAdmin && authUser != null) {
                ps.add(cb.equal(root.get("userId"), authUser.getId()));
            }
            if (filled(status) && !"all".equalsIgnoreCase(status)) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (filled(userId)) {
                ps.add(cb.equal(root.get("userId"), Long.valueOf(userId)));
            }
            if (filled(fromDate) && filled(toDate)) {
                ps.add(cb.between(root.get("createdAt"),
                        LocalDate.parse(fromDate).atStartOfDay(),
                        LocalDate.parse(toDate).atTime(LocalTime.MAX)));
            }
            // Admin list UI exposes free-text "Remarks" / "Adm Remarks" filters.
            if (filled(remarkFilter)) {
                ps.add(cb.like(cb.lower(root.get("remark")), "%" + remarkFilter.toLowerCase() + "%"));
            }
            if (filled(adminRemarkFilter)) {
                ps.add(cb.like(cb.lower(root.get("adminRemark")), "%" + adminRemarkFilter.toLowerCase() + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Sort sort = Sort.by(Sort.Direction.DESC, "id");

        if (export) {
            List<FundRequest> all = fundRequestRepository.findAll(spec, sort);
            boolean trimmed = "approved".equals(status) || isAdmin;
            List<Map<String, Object>> rows = all.stream()
                    .map(fr -> trimmed ? exportRow(fr) : row(fr, baseUrl))
                    .collect(Collectors.toList());
            return response.success("FundRequests fetched", rows);
        }

        int perPage = parseIntOr(payloadString("paginate"), 15);
        int pageParam = parseIntOr(payloadString("page"), 1);
        Page<FundRequest> page = fundRequestRepository.findAll(spec,
                PageRequest.of(Math.max(0, pageParam - 1), perPage, sort));
        Map<String, Object> body = Paginated.of(page);
        body.put("data", page.getContent().stream().map(fr -> row(fr, baseUrl)).collect(Collectors.toList()));
        return response.success("FundRequests fetched", body);
    }

    // POST fundrequest/pendingCount — pending requests (admins: all; others: own).
    @PostMapping("fundrequest/pendingCount")
    public ResponseEntity<Map<String, Object>> fundrequestPendingCount(
            @AuthenticationPrincipal CurrentUser currentUser) {
        User authUser = userOf(currentUser);
        boolean isAdmin = authUser != null && ADMIN_ROLES.contains(String.valueOf(authUser.getRole()));
        Specification<FundRequest> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("status"), "pending"));
            if (!isAdmin && authUser != null) {
                ps.add(cb.equal(root.get("userId"), authUser.getId()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pending_count", fundRequestRepository.count(spec));
        data.put("role", authUser == null ? null : authUser.getRole());
        return response.success("Pending fund request count fetched successfully", data);
    }

    // POST fundrequest/test — legacy debug route, kept for parity.
    @PostMapping("fundrequest/test")
    public ResponseEntity<Map<String, Object>> fundrequestTest() {
        return response.success("Success", payload());
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    /**
     * POST fundrequest/createFundRequest — allowed only 08:00–23:30 IST. Requires an existing
     * Account for the user and a creator role. Receipt may be sent as base64 chunks
     * (receipt1, receipt2, …) or a single data-URI.
     */
    @PostMapping("fundrequest/createFundRequest")
    public ResponseEntity<Map<String, Object>> fundrequestCreateFundRequest(
            @AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest http) {
        LocalTime now = LocalTime.now(IST);
        if (now.isBefore(LocalTime.of(8, 0)) || now.isAfter(LocalTime.of(23, 30))) {
            return response.error("Fund request allowed only between 8:00 AM to 11:30 PM.",
                    List.of(), HttpStatus.FORBIDDEN);
        }
        return create(currentUser, http, false);
    }

    /** POST fundrequest/createFundRequestnew — same as create, base64 chunk receipts, no time window. */
    @PostMapping("fundrequest/createFundRequestnew")
    public ResponseEntity<Map<String, Object>> fundrequestCreateFundRequestnew(
            @AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest http) {
        return create(currentUser, http, true);
    }

    private ResponseEntity<Map<String, Object>> create(CurrentUser currentUser, HttpServletRequest http,
                                                       boolean strictNew) {
        User user = userOf(currentUser);
        if (user == null) {
            return response.error("Unauthenticated.", "", HttpStatus.UNAUTHORIZED);
        }

        // --- validation (createFundRequest) ---
        Map<String, String> errors = new LinkedHashMap<>();
        for (String f : List.of("bank_name", "mode", "bank_ref_id", "date", "amount", "remark", "client_ref")) {
            if (!filled(payloadString(f))) errors.put(f, "The " + f.replace('_', ' ') + " field is required.");
        }
        BigDecimal amount = null;
        if (!errors.containsKey("amount")) {
            try {
                amount = new BigDecimal(payloadString("amount").trim());
            } catch (NumberFormatException e) {
                errors.put("amount", "The amount must be a number.");
            }
        }
        String clientRef = payloadString("client_ref");
        if (filled(clientRef) && fundRequestRepository.existsByClientRef(clientRef)) {
            errors.put("client_ref", "The client ref has already been taken.");
        }
        if (!errors.isEmpty()) {
            return response.error("Validation error", errors, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (accountRepository.findByUserId(user.getId()).isEmpty()) {
            return response.error("Not Authorised. Please contact your administrator.", List.of(),
                    HttpStatus.FORBIDDEN);
        }
        if (!CREATOR_ROLES.contains(String.valueOf(user.getRole()))) {
            return response.error("Operation Not Authorised", List.of(), HttpStatus.FORBIDDEN);
        }

        // --- receipt (optional) ---
        String receiptPath = receiptStorage.saveBase64Chunks(payload(), "receipt");
        if (receiptPath == null) {
            receiptPath = receiptStorage.saveDataUri(payloadString("receipt"), "receipt");
        }
        if (receiptPath == null && filled(payloadString("receipt")) && !strictNew) {
            return response.error("Invalid receipt file. Must be jpg, jpeg, png and <=2MB.",
                    List.of(), HttpStatus.UNPROCESSABLE_ENTITY);
        }

        FundRequest fr = new FundRequest();
        fr.setBankName(payloadString("bank_name"));
        fr.setReceipt(receiptPath);
        fr.setStatus("pending");
        fr.setAsmId(user.getParent());
        fr.setUserId(user.getId());
        fr.setUsername("TRANS" + user.getId());
        fr.setRole(user.getRole());
        fr.setName(user.getEstablishment());
        fr.setMode(payloadString("mode"));
        fr.setBankRefId(payloadString("bank_ref_id"));
        fr.setDate(payloadString("date"));
        fr.setAmount(amount);
        fr.setRemark(payloadString("remark"));
        fr.setClientRef(clientRef);

        try {
            FundRequest saved = fundRequestRepository.save(fr);
            audit(user, "CREATE_FUND_REQUEST", http, payloadWithoutReceipts(), "Success", saved.getId());
            return response.success("FundRequest Created Successfully", row(saved, null));
        } catch (Exception e) {
            receiptStorage.delete(receiptPath);
            return response.error("FundRequest Cannot Be Created", e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ------------------------------------------------------------------
    // Approve / reject
    // ------------------------------------------------------------------

    /**
     * POST fundrequest/updateFundRequest — adm/sadm only. status must be approved|rejected and the
     * request must still be pending. Approval credits the wallet, bumps the account balance
     * (credit-limit guarded for non-sadm) and writes an account statement atomically.
     */
    @PostMapping("fundrequest/updateFundRequest")
    public ResponseEntity<Map<String, Object>> fundrequestUpdateFundRequest(
            @AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest http) {
        User user = userOf(currentUser);
        Map<String, String> errors = new LinkedHashMap<>();
        String idStr = payloadString("id");
        String status = payloadString("status");
        if (!filled(idStr)) errors.put("id", "The id field is required.");
        else if (!fundRequestRepository.existsById(parseLong(idStr)))
            errors.put("id", "The selected id is invalid.");
        if (!filled(status)) errors.put("status", "The status field is required.");
        else if (!Set.of("approved", "rejected").contains(status))
            errors.put("status", "The selected status is invalid.");
        if (!errors.isEmpty()) {
            return response.error("Validation Failed", errors, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (user == null || !ADMIN_ROLES.contains(String.valueOf(user.getRole()))) {
            return response.error("Operation Not Authorised", "", HttpStatus.FORBIDDEN);
        }

        try {
            FundRequest updated = fundRequestService.decide(user, parseLong(idStr), status,
                    payloadString("admin_remark"));
            audit(user, "UPDATE_FUND_REQUEST", http, payload(), "Success", updated.getId());
            return response.success("FundRequest Updated Successfully", row(updated, null));
        } catch (FundRequestException e) {
            return response.error(e.getMessage(), e.httpStatus() == 400 || e.httpStatus() == 403 ? "" : List.of(),
                    HttpStatus.valueOf(e.httpStatus()));
        } catch (Exception e) {
            return response.error("Something went wrong: " + e.getMessage(), List.of(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ------------------------------------------------------------------
    // Edit / delete
    // ------------------------------------------------------------------

    // POST fundrequest/editFundRequest — owner roles only; only while pending; receipt replaceable.
    @PostMapping("fundrequest/editFundRequest")
    public ResponseEntity<Map<String, Object>> fundrequestEditFundRequest(
            @AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest http) {
        User user = userOf(currentUser);
        String idStr = payloadString("id");
        if (!filled(idStr) || !fundRequestRepository.existsById(parseLong(idStr))) {
            return response.error("Validation Failed", Map.of("id", "The selected id is invalid."),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (user == null || !EDITOR_ROLES.contains(String.valueOf(user.getRole()))) {
            return response.error("Operation Not Authorised", "", HttpStatus.FORBIDDEN);
        }
        FundRequest fr = fundRequestRepository.findById(parseLong(idStr)).orElse(null);
        if (fr == null) {
            return response.error("Request Not Found", "", HttpStatus.FORBIDDEN);
        }
        if (!"pending".equals(fr.getStatus())) {
            return response.error("Cannot update a request with status: " + fr.getStatus().toUpperCase(),
                    List.of(), HttpStatus.BAD_REQUEST);
        }

        // Replace receipt when a new one is supplied (chunks or data-URI).
        String newReceipt = receiptStorage.saveBase64Chunks(payload(), "receipt");
        if (newReceipt == null) newReceipt = receiptStorage.saveDataUri(payloadString("receipt"), "receipt");
        if (newReceipt != null) {
            receiptStorage.delete(fr.getReceipt());
            fr.setReceipt(newReceipt);
        }
        if (filled(payloadString("bank_name"))) fr.setBankName(payloadString("bank_name"));
        if (filled(payloadString("mode"))) fr.setMode(payloadString("mode"));
        if (filled(payloadString("bank_ref_id"))) fr.setBankRefId(payloadString("bank_ref_id"));
        if (filled(payloadString("date"))) fr.setDate(payloadString("date"));
        // The edit form posts "remarks" (plural); legacy API used "remark". Accept both.
        if (filled(payloadString("remark"))) fr.setRemark(payloadString("remark"));
        else if (filled(payloadString("remarks"))) fr.setRemark(payloadString("remarks"));

        FundRequest saved = fundRequestRepository.save(fr);
        audit(user, "EDIT_FUND_REQUEST", http, payloadWithoutReceipts(), "Success", saved.getId());
        return response.success("FundRequest Updated Successfully", row(saved, null));
    }

    // POST fundrequest/deleteFundRequest — NOT for owner roles; MPIN required; only while pending.
    @PostMapping("fundrequest/deleteFundRequest")
    public ResponseEntity<Map<String, Object>> fundrequestDeleteFundRequest(
            @AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest http) {
        User user = userOf(currentUser);
        Map<String, String> errors = new LinkedHashMap<>();
        String idStr = payloadString("id");
        String mpin = payloadString("mpin");
        if (!filled(idStr) || !fundRequestRepository.existsById(parseLong(idStr)))
            errors.put("id", "The selected id is invalid.");
        if (!filled(mpin) || mpin.length() < 4 || mpin.length() > 6)
            errors.put("mpin", "The mpin must be between 4 and 6 characters.");
        if (!errors.isEmpty()) {
            return response.error("Validation Failed", errors, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (user == null || EDITOR_ROLES.contains(String.valueOf(user.getRole()))) {
            return response.error("Operation Not Authorised", "", HttpStatus.UNAUTHORIZED);
        }
        if (!passwordService.matchesMpin(mpin, user.getMpin())) {
            return response.error("Invalid MPIN", List.of(), HttpStatus.FORBIDDEN);
        }
        FundRequest fr = fundRequestRepository.findById(parseLong(idStr)).orElse(null);
        if (fr == null) {
            return response.error("FundRequest Not Found", "", HttpStatus.NOT_FOUND);
        }
        if (!"pending".equals(fr.getStatus())) {
            return response.error("Cannot delete a request with status: " + fr.getStatus().toUpperCase(),
                    List.of(), HttpStatus.BAD_REQUEST);
        }
        receiptStorage.delete(fr.getReceipt());
        fundRequestRepository.delete(fr);
        audit(user, "DELETE_FUND_REQUEST", http, payload(), "Success", fr.getId());
        return response.success("FundRequest Deleted Successfully", List.of());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** Row shape for list/detail responses; rewrites receipt to the secure-images URL. */
    private Map<String, Object> row(FundRequest fr, String baseUrl) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", fr.getId());
        m.put("user_id", fr.getUserId());
        m.put("asm_id", fr.getAsmId());
        m.put("username", fr.getUsername());
        m.put("role", fr.getRole());
        m.put("name", fr.getName());
        m.put("bank_name", fr.getBankName());
        m.put("mode", fr.getMode());
        m.put("bank_ref_id", fr.getBankRefId());
        m.put("client_ref", fr.getClientRef());
        m.put("date", fr.getDate());
        m.put("amount", fr.getAmount());
        m.put("status", fr.getStatus());
        m.put("remark", fr.getRemark());
        m.put("admin_remark", fr.getAdminRemark());
        m.put("approved_by", fr.getApprovedBy());
        String receipt = fr.getReceipt();
        if (receipt != null && !receipt.isBlank()) {
            String file = receipt.substring(receipt.lastIndexOf('/') + 1);
            // Legacy parity: url('/secure-images/' . $filename) — root path, not under /fundrequest.
            receipt = (baseUrl == null ? "" : baseUrl) + "/secure-images/" + file;
        }
        m.put("receipt", receipt);
        m.put("created_at", fr.getCreatedAt());
        m.put("updated_at", fr.getUpdatedAt());
        return m;
    }

    /** Trimmed report row used for admin / approved exports (PHP export branch). */
    private Map<String, Object> exportRow(FundRequest fr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("created_at", fr.getCreatedAt());
        m.put("updated_at", fr.getUpdatedAt());
        m.put("user_id", fr.getUserId());
        m.put("shop_name", fr.getName());
        m.put("bank_name", fr.getBankName());
        m.put("mode", fr.getMode());
        m.put("bank_ref_id", fr.getBankRefId());
        m.put("amount", fr.getAmount());
        m.put("remark", fr.getRemark());
        m.put("admin_remark", fr.getAdminRemark());
        return m;
    }

    private Map<String, Object> payloadWithoutReceipts() {
        Map<String, Object> copy = new LinkedHashMap<>(payload());
        copy.keySet().removeIf(k -> k.equals("receipt") || k.matches("^receipt\\d+$"));
        return copy;
    }

    private void audit(User user, String action, HttpServletRequest http, Map<String, Object> req,
                       String status, Long fundRequestId) {
        try {
            auditLogService.record(user.getId(), user.getRole(), action, "fund_request",
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req),
                    "{\"fund_request_id\":" + fundRequestId + "}",
                    http.getRemoteAddr(), http.getHeader("User-Agent"), status);
        } catch (Exception ignored) {
            // auditing must never break the business flow
        }
    }

    private static Map<String, Object> field(String name, String label, String type, String placeholder,
                                              boolean required, Object options) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("label", label);
        f.put("type", type);
        if (placeholder != null) f.put("placeholder", placeholder);
        f.put("required", required);
        if (options != null) f.put("options", options);
        return f;
    }

    private static User userOf(CurrentUser cu) {
        return cu == null ? null : cu.user();
    }

    private static boolean filled(String v) {
        return v != null && !v.isBlank();
    }

    private static Long parseLong(String v) {
        try {
            return Long.valueOf(v.trim());
        } catch (Exception e) {
            return -1L;
        }
    }

    private static int parseIntOr(String v, int fallback) {
        try {
            return (v == null || v.isBlank()) ? fallback : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
