package com.securepay.modules.payout.controller;

import com.securepay.common.exception.ApiException;
import com.securepay.common.ratelimit.RateLimit;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.util.Json;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Paginated;
import com.securepay.common.web.Validation;
import com.securepay.integration.impsguru.ImpsguruClient;
import com.securepay.modules.payout.entity.PayoutSender;
import com.securepay.modules.payout.entity.PayoutTxn;
import com.securepay.modules.payout.entity.Purpose;
import com.securepay.modules.payout.repository.PayoutSenderRepository;
import com.securepay.modules.payout.repository.PayoutTxnRepository;
import com.securepay.modules.payout.repository.PurposeRepository;
import com.securepay.modules.payout.service.PayoutCtx;
import com.securepay.modules.payout.service.PayoutFlow;
import com.securepay.modules.payout.service.PayoutProviders;
import com.securepay.modules.payout.service.PayoutResponseHandler;
import com.securepay.modules.payout.service.PayoutRules;
import com.securepay.modules.payout.service.PayoutSupport;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Port of {@code Payout/Controllers/PayoutController}: transactions listing, the ImpsGuru /
 * Abhayam / Utkal bank payout ({@code payments/v2/payOut}), purposes and admin sender maintenance.
 */
@RestController
public class PayoutController extends BaseController {

    private static final List<String> BASE_COLS = List.of("id", "txn_id", "user_id", "pf", "mobile_number",
            "beneficiary_name", "account_number", "ifsc_code", "bank_name", "operator", "operator_id", "amount");
    private static final List<String> TAIL_COLS = List.of("purpose", "charges", "gst", "status", "mop",
            "created_at", "updated_at");

    private final ResponseFactory response;
    private final PayoutRules rules;
    private final PayoutSupport support;
    private final PayoutFlow flow;
    private final PayoutProviders providers;
    private final PayoutResponseHandler handler;
    private final ImpsguruClient impsguruClient;
    private final PayoutTxnRepository txnRepository;
    private final PayoutSenderRepository senderRepository;
    private final PurposeRepository purposeRepository;
    private final UserRepository userRepository;

    public PayoutController(ResponseFactory response, PayoutRules rules, PayoutSupport support, PayoutFlow flow,
                            PayoutProviders providers, PayoutResponseHandler handler,
                            ImpsguruClient impsguruClient, PayoutTxnRepository txnRepository,
                            PayoutSenderRepository senderRepository, PurposeRepository purposeRepository,
                            UserRepository userRepository) {
        this.response = response;
        this.rules = rules;
        this.support = support;
        this.flow = flow;
        this.providers = providers;
        this.handler = handler;
        this.impsguruClient = impsguruClient;
        this.txnRepository = txnRepository;
        this.senderRepository = senderRepository;
        this.purposeRepository = purposeRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------ transactions listing
    private List<Long> descendantIds(Long rootId) {
        List<Long> ids = new ArrayList<>();
        ids.add(rootId);
        Deque<Long> queue = new ArrayDeque<>(List.of(rootId));
        Set<Long> seen = new LinkedHashSet<>(ids);
        while (!queue.isEmpty()) {
            for (User child : userRepository.findByParent(queue.poll())) {
                if (seen.add(child.getId())) {
                    ids.add(child.getId());
                    queue.add(child.getId());
                }
            }
        }
        return ids;
    }

    private static boolean filled(Map<String, Object> p, String k) {
        return p.get(k) != null && !p.get(k).toString().isBlank();
    }

    private static boolean truthy(Object o) {
        if (o == null) {
            return false;
        }
        String s = o.toString().trim().toLowerCase();
        return s.equals("1") || s.equals("true") || s.equals("on") || s.equals("yes");
    }

    // POST payout/getPayoutTxns
    @PostMapping("payout/getPayoutTxns")
    public ResponseEntity<Map<String, Object>> getPayoutTxns(@AuthenticationPrincipal CurrentUser cu) {
        User user = cu.user();
        Map<String, Object> p = payload();
        boolean export = truthy(p.get("export"));
        List<String> cols = null; // null = every column
        List<Long> scope = null;  // null = no user restriction
        switch (user.getRole()) {
            case "sadm", "adm" -> {
                if (export) {
                    cols = List.of("id", "txn_id", "user_id", "mobile_number", "beneficiary_name", "account_number",
                            "ifsc_code", "bank_name", "route", "operator", "operator_id", "amount", "ret_comm",
                            "ret_tds", "di_comm", "di_tds", "md_comm", "md_tds", "a_comm", "purpose", "charges",
                            "gst", "status", "mop", "created_at", "updated_at");
                }
            }
            case "md" -> {
                scope = descendantIds(user.getId());
                cols = merge(BASE_COLS, List.of("ret_comm", "ret_tds", "di_comm", "di_tds", "md_comm", "md_tds"));
            }
            case "di" -> {
                scope = descendantIds(user.getId());
                cols = merge(BASE_COLS, List.of("ret_comm", "ret_tds", "di_comm", "di_tds"));
            }
            case "dd", "api", "ret" -> {
                scope = List.of(user.getId());
                cols = merge(BASE_COLS, List.of("ret_comm", "ret_tds"));
            }
            default -> {
                return response.error("Unauthorized role access", List.of(), HttpStatus.FORBIDDEN);
            }
        }
        final List<Long> scopeF = scope;
        String status = p.get("status") == null ? null : p.get("status").toString();
        if ("refund".equals(status)) {
            status = "refunded";
        }
        final String statusF = status;
        Specification<PayoutTxn> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (scopeF != null) {
                ps.add(root.get("userId").in(scopeF));
            }
            if (filled(p, "status")) {
                ps.add(cb.equal(root.get("status"), statusF.toUpperCase()));
            }
            if (filled(p, "mobile_number")) {
                ps.add(cb.equal(root.get("mobileNumber"), p.get("mobile_number").toString()));
            }
            if (filled(p, "account_number")) {
                ps.add(cb.equal(root.get("accountNumber"), p.get("account_number").toString()));
            }
            if (filled(p, "user_id")) {
                ps.add(cb.equal(root.get("userId"), Long.valueOf(p.get("user_id").toString().trim())));
            }
            if (filled(p, "route")) {
                ps.add(cb.equal(root.get("route"), p.get("route").toString()));
            }
            if (filled(p, "beneficiary_name")) {
                ps.add(cb.equal(root.get("beneficiaryName"), p.get("beneficiary_name").toString()));
            }
            if (filled(p, "txn_id")) {
                ps.add(cb.equal(root.get("txnId"), p.get("txn_id").toString()));
            }
            if (filled(p, "service")) {
                ps.add(cb.equal(root.get("operator"), p.get("service").toString()));
            }
            if (filled(p, "from_date") && filled(p, "to_date")) {
                ps.add(cb.between(root.get("createdAt"),
                        LocalDate.parse(p.get("from_date").toString().trim()).atStartOfDay(),
                        LocalDate.parse(p.get("to_date").toString().trim()).atTime(LocalTime.of(23, 59, 59))));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Sort latest = Sort.by(Sort.Direction.DESC, "createdAt");
        final List<String> colsF = cols;
        if (export) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (PayoutTxn t : txnRepository.findAll(spec, latest)) {
                rows.add(shape(t, colsF));
            }
            return response.success("Transactions fetched", rows);
        }
        int perPage = 15;
        int page = 1;
        try {
            if (filled(p, "paginate")) {
                perPage = Math.max(1, Integer.parseInt(p.get("paginate").toString().trim()));
            }
            if (filled(p, "page")) {
                page = Math.max(1, Integer.parseInt(p.get("page").toString().trim()));
            }
        } catch (NumberFormatException ignored) {
            // keep defaults
        }
        Page<PayoutTxn> result = txnRepository.findAll(spec, PageRequest.of(page - 1, perPage, latest));
        Map<String, Object> body = Paginated.of(result);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PayoutTxn t : result.getContent()) {
            rows.add(shape(t, colsF));
        }
        body.put("data", rows);
        return response.success("Transactions fetched", body);
    }

    private static List<String> merge(List<String> a, List<String> mid) {
        List<String> out = new ArrayList<>(a);
        out.addAll(mid);
        out.addAll(TAIL_COLS);
        return out;
    }

    /** Applies the role's column list and appends {@code establishment} like the PHP transform. */
    private Map<String, Object> shape(PayoutTxn t, List<String> cols) {
        Map<String, Object> full = Json.readMap(Json.write(t));
        Map<String, Object> out;
        if (cols == null) {
            out = full;
        } else {
            out = new LinkedHashMap<>();
            for (String c : cols) {
                out.put(c, full.get(c));
            }
        }
        String est = userRepository.findById(t.getUserId()).map(User::getEstablishment).orElse(null);
        out.put("establishment", est == null ? "Unknown" : est);
        return out;
    }

    // ------------------------------------------------------------------ schema
    // POST payments/v2/getPayoutSchema
    @PostMapping("payments/v2/getPayoutSchema")
    public ResponseEntity<Map<String, Object>> getPayoutSchema() {
        Map<String, Object> mop = new LinkedHashMap<>();
        mop.put("name", "mop");
        mop.put("label", "Mode of Payment");
        mop.put("type", "select");
        mop.put("placeholder", "Select mode of payment");
        mop.put("required", true);
        mop.put("options", List.of("UPI", "NEFT", "IMPS", "RTGS"));
        mop.put("validation", SenderController.orderedMap("type", "string",
                "enum", List.of("UPI", "NEFT", "IMPS", "RTGS")));
        Map<String, Object> amount = SenderController.field("amount", "Amount", "number", "Enter amount", true,
                SenderController.orderedMap("type", "numeric", "min", 10, "max", 100000));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("formName", "Payout Schema");
        schema.put("fields", List.of(mop, amount));
        return response.success("Schema fetched", schema);
    }

    // ------------------------------------------------------------------ payout
    // POST payments/v2/payOut and payments/v2/userPayOut (throttle:1,0.5)
    @PostMapping({"payments/v2/payOut", "payments/v2/userPayOut"})
    @RateLimit(requests = 1, seconds = 30)
    public ResponseEntity<Map<String, Object>> payOut(@AuthenticationPrincipal CurrentUser cu,
                                                      HttpServletRequest request) {
        User user = cu.user();
        PayoutCtx c = PayoutCtx.of(payload(), request);
        flow.consumeUuid(c, "payOut1");
        Validation v = rules.payOut(c.p(), user);
        if (v.fails()) {
            return response.error("Validation failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        PayoutFlow.Spec spec = new PayoutFlow.Spec(
                true,
                mobile -> senderRepository.findFirstByMobileNumberAndIsActive(mobile, 1).orElse(null),
                null,
                true,
                (txn, service, u, sender) -> router(txn, service, u, sender, c),
                (txn, resp, u, service, sender) -> {
                    String route = service.getRoute() == null ? "" : service.getRoute().toUpperCase();
                    return switch (route) {
                        case "IMGURU", "IMPSGURU" -> handler.handle(txn, resp, u, service, sender);
                        case "ABHAYAM", "UTKAL" -> handler.handleResponseAbhyam(txn, resp, u, service, sender);
                        default -> throw new ApiException("Invalid Route in helper", HttpStatus.NOT_FOUND, "");
                    };
                });
        return flow.run(user, c, spec);
    }

    /** Port of {@code PayoutController::router}. */
    private Map<String, Object> router(PayoutTxn txn, com.securepay.modules.service.entity.ServiceEntity service,
                                       User user, PayoutSender sender, PayoutCtx c) {
        String route = service.getRoute() == null ? "" : service.getRoute().toUpperCase();
        switch (route) {
            case "IMGURU", "IMPSGURU" -> {
                return impsguruClient.payout(new ImpsguruClient.PayoutData(
                        txn.getMobileNumber(), txn.getAmount().toPlainString(), txn.getAccountNumber(),
                        txn.getBeneficiaryName(), txn.getIfscCode(), txn.getMop(), txn.getTxnId(),
                        txn.getIfscCode(), c.s("latitude"), c.s("longitude")));
            }
            case "ABHAYAM" -> {
                return providers.abhayam(PayoutProviders.AB_SINGLE_TXN, bankPayload(false, txn, user, sender, c, "27", "SAND"));
            }
            case "UTKAL" -> {
                return providers.utkal(PayoutProviders.UT_SINGLE_TXN, bankPayload(true, txn, user, sender, c, "27", "MAGNI"));
            }
            default -> {
                return null; // PHP returns an error response object here; handler then answers "Invalid Route"
            }
        }
    }

    private Map<String, Object> bankPayload(boolean utkal, PayoutTxn txn, User user, PayoutSender sender,
                                            PayoutCtx c, String serviceId, String defaultBankCode) {
        Map<String, Object> m = providers.auth(utkal);
        m.put("RequestID", txn.getTxnId());
        m.put("ServiceId", utkal ? (Object) Integer.valueOf(serviceId) : serviceId);
        m.put("SenderMobile", c.s("mobile_number"));
        m.put("SenderName", sender.getSenderName());
        m.put("BankName", c.s("bank_name"));
        m.put("BankCode", c.s("bank_code") != null ? c.s("bank_code") : defaultBankCode);
        m.put("BankAccountNumber", c.s("account_number"));
        m.put("BeneficiaryName", c.s("beneficiary_name"));
        m.put("BankIfsc", c.s("ifsc_code"));
        m.put("Amount", c.s("amount"));
        m.put("AdharNo", c.s("adhar_no") != null ? c.s("adhar_no") : "123123123123");
        m.put("PanNo", user.getPan() == null ? "" : user.getPan());
        m.put("Lat", c.s("latitude") != null ? c.s("latitude") : "22.753");
        m.put("Long", c.s("longitude") != null ? c.s("longitude") : "78.773");
        return m;
    }

    // POST payout/upiVerifiaction — switched off in the source (unreachable code after the return)
    @PostMapping("payout/upiVerifiaction")
    public ResponseEntity<Map<String, Object>> upiVerifiaction() {
        return response.error("This Service Not Active", "", HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------ purposes
    // POST payout/createPurpose
    @PostMapping("payout/createPurpose")
    public ResponseEntity<Map<String, Object>> createPurpose(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("type").required().string();
        v.field("description").sometimes().string().min(2);
        if (v.fails()) {
            return response.error("Validation failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!PayoutSupport.hasRole(cu.user(), "sadm", "adm")) {
            return response.error("unauthorized", List.of(), HttpStatus.FORBIDDEN);
        }
        Purpose purpose = new Purpose();
        purpose.setType(p.get("type").toString());
        purpose.setDescription(Objects.toString(p.get("description"), null));
        purpose = purposeRepository.save(purpose);
        return response.success("Purpose created successfully", purpose, HttpStatus.CREATED);
    }

    // POST payout/getPurpose
    @PostMapping("payout/getPurpose")
    public ResponseEntity<Map<String, Object>> getPurpose() {
        return response.success("Purpose fetch successfully", purposeRepository.findAll());
    }

    // ------------------------------------------------------------------ admin sender maintenance
    // POST payout/get_payout_senderName
    @PostMapping("payout/get_payout_senderName")
    public ResponseEntity<Map<String, Object>> getPayoutSenderName(@AuthenticationPrincipal CurrentUser cu) {
        if (!PayoutSupport.hasRole(cu.user(), "sadm", "adm")) {
            return response.error("Unauthorized", List.of(), HttpStatus.FORBIDDEN);
        }
        Map<String, Object> p = payload();
        Specification<PayoutSender> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (filled(p, "sender_name")) {
                ps.add(cb.equal(root.get("senderName"), p.get("sender_name").toString()));
            }
            if (filled(p, "mobile_number")) {
                ps.add(cb.equal(root.get("mobileNumber"), p.get("mobile_number").toString()));
            }
            if (filled(p, "pipe")) {
                ps.add(cb.equal(root.get("pipe"), p.get("pipe").toString()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        if (filled(p, "export") && "1".equals(p.get("export").toString())) {
            return response.success("Payout senders fetched successfully", senderRepository.findAll(spec));
        }
        int perPage = 15;
        int page = 1;
        try {
            if (filled(p, "paginate")) {
                perPage = Math.max(1, Integer.parseInt(p.get("paginate").toString().trim()));
            }
            if (filled(p, "page")) {
                page = Math.max(1, Integer.parseInt(p.get("page").toString().trim()));
            }
        } catch (NumberFormatException ignored) {
            // keep defaults
        }
        return response.success("Payout senders fetched successfully",
                Paginated.of(senderRepository.findAll(spec, PageRequest.of(page - 1, perPage))));
    }

    private Validation idRule(Map<String, Object> p) {
        Validation v = Validation.of(p);
        v.field("id").required().integer();
        return v;
    }

    // POST payout/update_payout_sender
    @PostMapping("payout/update_payout_sender")
    public ResponseEntity<Map<String, Object>> updatePayoutSender(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = idRule(p);
        if (v.fails()) {
            return response.errorWithObjectMessage(v.errors(), "Validation Error", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!PayoutSupport.hasRole(cu.user(), "sadm", "adm")) {
            return response.error("Unauthorised Action", List.of(), HttpStatus.FORBIDDEN);
        }
        PayoutSender s = senderRepository.findById(Long.valueOf(p.get("id").toString().trim())).orElse(null);
        if (s == null) {
            return response.error("Payout sender not found", "", HttpStatus.FORBIDDEN);
        }
        if (p.get("sender_name") != null) {
            s.setSenderName(p.get("sender_name").toString());
        }
        if (p.get("mobile_number") != null) {
            s.setMobileNumber(p.get("mobile_number").toString());
        }
        if (p.get("rem_limit") != null) {
            s.setRemLimit(new java.math.BigDecimal(p.get("rem_limit").toString().trim()));
        }
        s = senderRepository.save(s);
        return response.success("Payout sender updated Successfully", s);
    }

    // POST payout/delete_payout_sender
    @PostMapping("payout/delete_payout_sender")
    public ResponseEntity<Map<String, Object>> deletePayoutSender(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = idRule(p);
        if (v.fails()) {
            return response.error("Validation Failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!PayoutSupport.hasRole(cu.user(), "sadm", "adm")) {
            return response.error("Unauthorised Action", List.of(), HttpStatus.FORBIDDEN);
        }
        PayoutSender s = senderRepository.findById(Long.valueOf(p.get("id").toString().trim())).orElse(null);
        if (s == null) {
            return response.error("Payout sender not found", "", HttpStatus.FORBIDDEN);
        }
        senderRepository.delete(s);
        return response.success("Payout sender deleted Successfully", List.of());
    }
}
