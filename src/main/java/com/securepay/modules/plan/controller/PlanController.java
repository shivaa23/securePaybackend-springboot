package com.securepay.modules.plan.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Validation;
import com.securepay.modules.plan.entity.Plan;
import com.securepay.modules.plan.repository.PlanRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Port of {@code Plan/Controllers/PlanController}. */
@RestController
public class PlanController extends BaseController {

    private final ResponseFactory response;
    private final PlanRepository planRepo;
    private final UserRepository userRepo;

    public PlanController(ResponseFactory response, PlanRepository planRepo, UserRepository userRepo) {
        this.response = response;
        this.planRepo = planRepo;
        this.userRepo = userRepo;
    }

    private static boolean isAdmin(CurrentUser cu) {
        return "sadm".equals(cu.user().getRole()) || "adm".equals(cu.user().getRole());
    }

    private static String s(Map<String, Object> p, String k) {
        return p.get(k) == null ? null : p.get(k).toString();
    }

    private ResponseEntity<Map<String, Object>> denied() {
        return response.error("Action not Authorised", "", HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<Map<String, Object>> invalid(Validation v) {
        return response.error("Validation Failed", v.errors(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // POST plan/getPlans — filters apply whenever the key is present in the request
    @PostMapping("plan/getPlans")
    public ResponseEntity<Map<String, Object>> getPlans(@AuthenticationPrincipal CurrentUser cu) {
        if (!isAdmin(cu)) {
            return denied();
        }
        Map<String, Object> p = payload();
        List<Plan> plans = new ArrayList<>();
        for (Plan pl : planRepo.findAll()) {
            if (p.containsKey("id") && !String.valueOf(pl.getId()).equals(s(p, "id"))) {
                continue;
            }
            if (p.containsKey("name") && (pl.getName() == null || !pl.getName().equals(s(p, "name")))) {
                continue;
            }
            plans.add(pl);
        }
        return response.success("Plans fetched", plans);
    }

    // POST plan/getPlanSchema
    @PostMapping("plan/getPlanSchema")
    public ResponseEntity<Map<String, Object>> getPlanSchema() {
        Map<String, Object> name = field("name", "Plan Name", "text", "Enter plan name", 3, 255);
        Map<String, Object> desc = field("description", "Plan Description", "textarea", "Enter plan description",
                2, 1000);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("formName", "Create Plan");
        schema.put("fields", List.of(name, desc));
        return response.success("Schema fetched", schema);
    }

    private static Map<String, Object> field(String n, String label, String type, String ph, int min, int max) {
        Map<String, Object> val = new LinkedHashMap<>();
        val.put("type", "string");
        val.put("minLength", min);
        val.put("maxLength", max);
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", n);
        f.put("label", label);
        f.put("type", type);
        f.put("placeholder", ph);
        f.put("required", true);
        f.put("validation", val);
        return f;
    }

    // POST plan/createPlan
    @PostMapping("plan/createPlan")
    public ResponseEntity<Map<String, Object>> createPlan(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("name").required().string().max(255);
        v.field("description").required().string().min(10).max(1000);
        if (v.fails()) {
            return invalid(v);
        }
        if (!isAdmin(cu)) {
            return denied();
        }
        Plan plan = new Plan();
        plan.setName(s(p, "name"));
        plan.setDescription(s(p, "description"));
        return response.success("Plan Created Successfully", planRepo.save(plan));
    }

    // POST plan/updatePlan
    @PostMapping("plan/updatePlan")
    public ResponseEntity<Map<String, Object>> updatePlan(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("id").required().integer();
        v.field("name").sometimes().string().max(255);
        v.field("description").sometimes().string().min(10).max(1000);
        if (v.fails()) {
            return invalid(v);
        }
        if (!isAdmin(cu)) {
            return denied();
        }
        Plan plan = planRepo.findById(Long.valueOf(s(p, "id").trim())).orElse(null);
        if (plan == null) {
            return response.error("Something Went Wrong", "", HttpStatus.FORBIDDEN);
        }
        if (p.get("name") != null) {
            plan.setName(s(p, "name"));
        }
        if (p.get("description") != null) {
            plan.setDescription(s(p, "description"));
        }
        return response.success("Plan updated Successfully", planRepo.save(plan));
    }

    // POST plan/asignPlan
    @PostMapping("plan/asignPlan")
    public ResponseEntity<Map<String, Object>> asignPlan(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("user_id").required().integer().exists(id -> userRepo.existsById(Long.valueOf(id.trim())));
        v.field("plan_id").required().integer().exists(id -> planRepo.existsById(Long.valueOf(id.trim())));
        if (v.fails()) {
            return invalid(v);
        }
        if (!isAdmin(cu)) {
            return denied();
        }
        User user = userRepo.findById(Long.valueOf(s(p, "user_id").trim())).orElse(null);
        if (user == null) {
            return response.error("User not found", "", HttpStatus.NOT_FOUND);
        }
        user.setPlanId(Long.valueOf(s(p, "plan_id").trim()));
        user = userRepo.save(user);
        // Return a safe projection (never the password / MPIN hashes the Eloquent model hides).
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", user.getId());
        out.put("name", user.getName());
        out.put("mobile", user.getMobile());
        out.put("role", user.getRole());
        out.put("plan_id", user.getPlanId());
        return response.success("Plan updated successfully", out);
    }

    // POST plan/deletePlan
    @PostMapping("plan/deletePlan")
    public ResponseEntity<Map<String, Object>> deletePlan(@AuthenticationPrincipal CurrentUser cu) {
        Map<String, Object> p = payload();
        Validation v = Validation.of(p);
        v.field("id").required().integer().exists(id -> planRepo.existsById(Long.valueOf(id.trim())));
        if (v.fails()) {
            return invalid(v);
        }
        if (!isAdmin(cu)) {
            return denied();
        }
        Long id = Long.valueOf(s(p, "id").trim());
        if (userRepo.existsByPlanId(id)) {
            return response.error("This plan is currently assigned to one or more users and cannot be deleted..",
                    "", HttpStatus.FORBIDDEN);
        }
        planRepo.deleteById(id);
        return response.success("Plan deleted successfully.", "");
    }
}
