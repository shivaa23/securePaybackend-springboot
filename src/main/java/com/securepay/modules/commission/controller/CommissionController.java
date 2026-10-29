package com.securepay.modules.commission.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Validation;
import com.securepay.modules.admin.repository.RouteRepository;
import com.securepay.modules.commission.entity.AcommRule;
import com.securepay.modules.commission.entity.CommRule;
import com.securepay.modules.commission.repository.AcommRuleRepository;
import com.securepay.modules.commission.repository.CommRuleRepository;
import com.securepay.modules.plan.repository.PlanRepository;
import com.securepay.modules.service.repository.ServiceRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of {@code CommissionController} + {@code AdminCommissionController} (retailer-side
 * {@code comm_rules} and platform-side {@code acomm_rules}), including their validation rules,
 * schemas and the source's quirks (noted inline).
 */
@RestController
public class CommissionController extends BaseController {

    private final ResponseFactory response;
    private final CommRuleRepository commRepo;
    private final AcommRuleRepository acommRepo;
    private final PlanRepository planRepo;
    private final ServiceRepository serviceRepo;
    private final RouteRepository routeRepo;
    private final JdbcTemplate jdbc;

    public CommissionController(ResponseFactory response, CommRuleRepository commRepo, AcommRuleRepository acommRepo,
                                PlanRepository planRepo, ServiceRepository serviceRepo, RouteRepository routeRepo,
                                JdbcTemplate jdbc) {
        this.response = response;
        this.commRepo = commRepo;
        this.acommRepo = acommRepo;
        this.planRepo = planRepo;
        this.serviceRepo = serviceRepo;
        this.routeRepo = routeRepo;
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ helpers
    private static boolean isAdmin(CurrentUser cu) {
        return "sadm".equals(cu.user().getRole()) || "adm".equals(cu.user().getRole());
    }

    private static String s(Map<String, Object> p, String k) {
        return p.get(k) == null ? null : p.get(k).toString();
    }

    private static boolean filled(Map<String, Object> p, String k) {
        return p.get(k) != null && !p.get(k).toString().isBlank();
    }

    private static BigDecimal dec(Map<String, Object> p, String k) {
        return filled(p, k) ? new BigDecimal(s(p, k).trim()) : null;
    }

    private static Long lng(Map<String, Object> p, String k) {
        return filled(p, k) ? Long.valueOf(s(p, k).trim()) : null;
    }

    /** {@code gte:min_amount}: fails when min_amount is missing/non-numeric or max &lt; min. */
    private static boolean gteMin(Map<String, Object> p, String max) {
        try {
            return new BigDecimal(max.trim()).compareTo(new BigDecimal(s(p, "min_amount").trim())) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static final String GTE_MSG = "The max amount field must be greater than or equal to min amount.";

    private ResponseEntity<Map<String, Object>> denied() {
        return response.error("Action not Authorised", "", HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<Map<String, Object>> invalid(Validation v) {
        return response.error("Validation Error", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private static Map<String, Object> field(String name, String label, String type, String placeholder,
                                             Object options, Map<String, Object> validation) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("label", label);
        f.put("type", type);
        f.put("placeholder", placeholder);
        if (options != null) {
            f.put("options", options);
        }
        f.put("required", true);
        f.put("validation", validation);
        return f;
    }

    private static Map<String, Object> v(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i].toString(), kv[i + 1]);
        }
        return m;
    }

    private List<Object> amountFields() {
        return List.of(
                field("min_amount", "Minimum Amount", "number", "Enter minimum transaction amount", null,
                        v("type", "numeric", "min", 0)),
                field("max_amount", "Maximum Amount", "number", "Enter maximum transaction amount", null,
                        v("type", "numeric", "gte", "min_amount")));
    }

    // ------------------------------------------------------------------ comm_rules
    // POST commission/getCommissions
    @PostMapping("commission/getCommissions")
    public ResponseEntity<Map<String, Object>> getCommissions(@AuthenticationPrincipal CurrentUser cu) {
        if (!isAdmin(cu)) {
            return denied();
        }
        Map<String, Object> p = payload();
        boolean hasPlan = filled(p, "plan_id") && !"0".equals(s(p, "plan_id"));
        if (!hasPlan && !filled(p, "service_name") && !filled(p, "rule_type")) {
            return response.success("No Filters Applied", List.of());
        }
        Specification<CommRule> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (hasPlan) {
                ps.add(cb.equal(root.get("planId"), lng(p, "plan_id")));
            }
            if (filled(p, "service_name")) {
                ps.add(cb.equal(root.get("serviceName"), s(p, "service_name")));
            }
            if (filled(p, "rule_type")) {
                ps.add(cb.equal(root.get("ruleType"), s(p, "rule_type")));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return response.success("Commission Charge Rules fetched", commRepo.findAll(spec));
    }

    // POST commission/getCommissionSchema
    @PostMapping("commission/getCommissionSchema")
    public ResponseEntity<Map<String, Object>> getCommissionSchema() {
        List<Object> fields = new ArrayList<>();
        fields.add(field("plan_id", "Plan", "dropdown", "select your plan", planRepo.findAll(),
                v("type", "integer", "exists", "plans,id")));
        fields.add(field("service_name", "Service Name", "autocomplete", "Enter service name",
                serviceRepo.findAll(Sort.by("name")), v("type", "string", "maxLength", 255)));
        fields.add(field("rule_type", "Rule Type", "dropdown", "Select rule type",
                List.of("commission", "charge"), v("type", "string")));
        fields.add(field("value_type", "Value Type", "dropdown", "Select value type",
                List.of("percentage", "flat"), v("type", "string")));
        fields.add(field("comm_di", "Distributor Commission", "number", "Enter distributor commission", null,
                v("type", "numeric")));
        fields.add(field("comm_ret", "Retailer Commission", "number", "Enter retailer commission", null,
                v("type", "numeric")));
        fields.add(field("comm_dd", "Direct Dealer", "number", "Enter direct commission", null,
                v("type", "numeric")));
        fields.add(field("comm_md", "Master Distributor Commission", "number",
                "Enter master distributor commission", null, v("type", "numeric")));
        fields.addAll(amountFields());
        return response.success("Schema found", v("formName", "Commission", "fields", fields));
    }

    // POST commission/createCommissionRule
    @PostMapping("commission/createCommissionRule")
    public ResponseEntity<Map<String, Object>> createCommissionRule(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation val = Validation.of(p);
        val.field("plan_id").required().integer().exists(id -> planRepo.existsById(Long.valueOf(id.trim())));
        val.field("service_name").required().string().max(255);
        val.field("rule_type").required().string();
        val.field("value_type").required().string();
        for (String k : List.of("comm_dd", "comm_ret", "comm_di", "comm_md")) {
            val.field(k).required().numeric();
        }
        val.field("min_amount").required().numeric().min(0);
        val.field("max_amount").required().numeric().rule("gte", x -> gteMin(p, x), GTE_MSG);
        if (val.fails()) {
            return invalid(val);
        }
        if (!isAdmin(cu)) {
            return denied();
        }
        CommRule c = new CommRule();
        c.setPlanId(lng(p, "plan_id"));
        c.setServiceName(s(p, "service_name"));
        c.setRuleType(s(p, "rule_type"));
        c.setValueType(s(p, "value_type"));
        c.setCommDd(dec(p, "comm_dd"));
        c.setCommRet(dec(p, "comm_ret"));
        c.setCommDi(dec(p, "comm_di"));
        c.setCommMd(dec(p, "comm_md"));
        c.setMinAmount(dec(p, "min_amount"));
        c.setMaxAmount(dec(p, "max_amount"));
        return response.success("Commission Rule Created Successfully", commRepo.save(c));
    }

    // POST commission/updateCommissionRule
    @PostMapping("commission/updateCommissionRule")
    public ResponseEntity<Map<String, Object>> updateCommissionRule(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation val = updateRules(p, true);
        if (val.fails()) {
            return invalid(val);
        }
        if (!isAdmin(cu)) {
            return denied();
        }
        CommRule c = commRepo.findById(lng(p, "id")).orElse(null);
        if (c == null) {
            return response.error("Not found with this id ", "", HttpStatus.FORBIDDEN);
        }
        if (filled(p, "plan_id")) c.setPlanId(lng(p, "plan_id"));
        if (p.get("service_name") != null) c.setServiceName(s(p, "service_name"));
        if (p.get("rule_type") != null) c.setRuleType(s(p, "rule_type"));
        if (p.get("value_type") != null) c.setValueType(s(p, "value_type"));
        if (filled(p, "comm_dd")) c.setCommDd(dec(p, "comm_dd"));
        if (filled(p, "comm_ret")) c.setCommRet(dec(p, "comm_ret"));
        if (filled(p, "comm_di")) c.setCommDi(dec(p, "comm_di"));
        if (filled(p, "comm_md")) c.setCommMd(dec(p, "comm_md"));
        if (filled(p, "min_amount")) c.setMinAmount(dec(p, "min_amount"));
        if (filled(p, "max_amount")) c.setMaxAmount(dec(p, "max_amount"));
        return response.success("Commission Rule Updated Successfully", commRepo.save(c));
    }

    /** {@code CommissionValidation::updateCommissionRule} (also reused by the admin update, as in the source). */
    private Validation updateRules(Map<String, Object> p, boolean withComm) {
        Validation val = Validation.of(p);
        val.field("id").required().integer();
        val.field("plan_id").sometimes().integer().exists(id -> planRepo.existsById(Long.valueOf(id.trim())));
        val.field("service_name").sometimes().string().max(255);
        val.field("rule_type").sometimes().string();
        val.field("value_type").sometimes().string();
        for (String k : List.of("comm_dd", "comm_ret", "comm_di", "comm_md")) {
            val.field(k).sometimes().numeric();
        }
        val.field("min_amount").sometimes().numeric().min(0);
        val.field("max_amount").sometimes().numeric().rule("gte", x -> gteMin(p, x), GTE_MSG);
        return val;
    }

    // POST commission/deleteCommissionRule — the source has no role check here
    @PostMapping("commission/deleteCommissionRule")
    public ResponseEntity<Map<String, Object>> deleteCommissionRule() {
        Map<String, Object> p = payload();
        Validation val = deleteRules(p);
        if (val.fails()) {
            return invalid(val);
        }
        commRepo.deleteById(lng(p, "id"));
        return response.success("Commission Rule deleted  Successfully", List.of());
    }

    /** {@code deleteCommissionRule}: id must exist in comm_rules (the admin delete reuses it, as in the source). */
    private Validation deleteRules(Map<String, Object> p) {
        Validation val = Validation.of(p);
        val.field("id").required().integer().exists(id -> commRepo.existsById(Long.valueOf(id.trim())));
        return val;
    }

    // ------------------------------------------------------------------ acomm_rules
    // POST commission/getAdminCommissions
    @PostMapping("commission/getAdminCommissions")
    public ResponseEntity<Map<String, Object>> getAdminCommissions(@AuthenticationPrincipal CurrentUser cu) {
        if (!isAdmin(cu)) {
            return denied();
        }
        Map<String, Object> p = payload();
        Specification<AcommRule> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (filled(p, "service_name")) {
                ps.add(cb.equal(root.get("serviceName"), s(p, "service_name")));
            }
            if (filled(p, "rule_type")) {
                ps.add(cb.equal(root.get("ruleType"), s(p, "rule_type")));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return response.success("Commission Charge Rules fetched", acommRepo.findAll(spec));
    }

    // POST commission/getAdminCommissionSchema
    @PostMapping("commission/getAdminCommissionSchema")
    public ResponseEntity<Map<String, Object>> getAdminCommissionSchema() {
        List<Object> fields = new ArrayList<>();
        fields.add(field("service_name", "Service Name", "autocomplete", "Enter service name",
                serviceRepo.findAll(Sort.by("name")), v("type", "string", "maxLength", 255)));
        fields.add(field("route", "Route", "autocomplete", "Enter service name", routeRepo.findAll(),
                v("type", "string", "maxLength", 255)));
        fields.add(field("rule_type", "Rule Type", "dropdown", "Select rule type",
                List.of("commission", "charge"), v("type", "string")));
        fields.add(field("value_type", "Value Type", "dropdown", "Select value type",
                List.of("percentage", "flat"), v("type", "string")));
        fields.add(field("a_comm", "Admin Commission", "number", "Enter admin commission", null,
                v("type", "numeric")));
        fields.addAll(amountFields());
        return response.success("Schema found", v("formName", "Commission", "fields", fields));
    }

    // POST commission/createAdminCommissionRule
    @PostMapping("commission/createAdminCommissionRule")
    public ResponseEntity<Map<String, Object>> createAdminCommissionRule(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation val = Validation.of(p);
        val.field("service_name").required().string().max(255);
        val.field("rule_type").required().string();
        val.field("value_type").required().string();
        val.field("a_comm").required().numeric();
        val.field("min_amount").required().numeric().min(0);
        val.field("max_amount").required().numeric().rule("gte", x -> gteMin(p, x), GTE_MSG);
        if (val.fails()) {
            return invalid(val);
        }
        if (!isAdmin(cu)) {
            return denied();
        }
        AcommRule a = new AcommRule();
        a.setServiceName(s(p, "service_name"));
        a.setRuleType(s(p, "rule_type"));
        a.setRoute(s(p, "route"));
        a.setValueType(s(p, "value_type"));
        a.setAComm(dec(p, "a_comm"));
        a.setMinAmount(dec(p, "min_amount"));
        a.setMaxAmount(dec(p, "max_amount"));
        return response.success("Commission Rule Created Successfully", acommRepo.save(a));
    }

    // POST commission/updateAdminCommissionRule — validated with the retailer update rules, as in the source
    @PostMapping("commission/updateAdminCommissionRule")
    public ResponseEntity<Map<String, Object>> updateAdminCommissionRule(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation val = updateRules(p, false);
        if (val.fails()) {
            return invalid(val);
        }
        if (!isAdmin(cu)) {
            return denied();
        }
        AcommRule a = acommRepo.findById(lng(p, "id")).orElse(null);
        if (a == null) {
            // the PHP code dereferences null here (HTTP 500)
            return response.error("Something Went Wrong", "", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (p.get("service_name") != null) a.setServiceName(s(p, "service_name"));
        if (p.get("rule_type") != null) a.setRuleType(s(p, "rule_type"));
        if (p.get("route") != null) a.setRoute(s(p, "route"));
        if (p.get("value_type") != null) a.setValueType(s(p, "value_type"));
        if (filled(p, "a_comm")) a.setAComm(dec(p, "a_comm"));
        if (filled(p, "min_amount")) a.setMinAmount(dec(p, "min_amount"));
        if (filled(p, "max_amount")) a.setMaxAmount(dec(p, "max_amount"));
        return response.success("Commission Rule Updated Successfully", acommRepo.save(a));
    }

    // POST commission/deleteAdminCommissionRule — id is validated against comm_rules, as in the source
    @PostMapping("commission/deleteAdminCommissionRule")
    public ResponseEntity<Map<String, Object>> deleteAdminCommissionRule() {
        Map<String, Object> p = payload();
        Validation val = deleteRules(p);
        if (val.fails()) {
            return invalid(val);
        }
        Long id = lng(p, "id");
        if (!acommRepo.existsById(id)) {
            return response.error("Something Went Wrong", "", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        acommRepo.deleteById(id);
        return response.success("Commission Rule deleted  Successfully", List.of());
    }

    // POST commission/disburseCommRecharge — no auth in the source; returns the LAST user's figures only
    @PostMapping("commission/disburseCommRecharge")
    public ResponseEntity<Map<String, Object>> disburseCommRecharge() {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Long> ids = jdbc.queryForList("select id from users where role in ('api','ret','dd') order by id",
                Long.class);
        for (Long id : ids) {
            BigDecimal sum = jdbc.queryForObject("select coalesce(sum(ret_comm),0) from recharge_txns "
                    + "where status in ('SUCCESS','PENDING') and user_id = ?", BigDecimal.class, id);
            data.put("user_id", id);
            data.put("ret_comm", sum);
        }
        return ResponseEntity.ok(data);
    }
}
