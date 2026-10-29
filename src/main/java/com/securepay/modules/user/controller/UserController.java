package com.securepay.modules.user.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Paginated;
import com.securepay.modules.plan.entity.Plan;
import com.securepay.modules.plan.repository.PlanRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserController — ports the user module endpoints at their exact paths (doc 05).
 */
@RestController
public class UserController extends BaseController {

    private final ResponseFactory response;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    public UserController(ResponseFactory response, UserRepository userRepository,
                          PlanRepository planRepository) {
        this.response = response;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
    }

    // POST user/createUser
    @PostMapping("user/createUser")
    public ResponseEntity<Map<String, Object>> userCreateUser() {
        return response.success("Success", payload());
    }

    /**
     * POST user/getUsers — port of the legacy UserController::getUsers (doc 05).
     *
     * <p>Role scoping: md/di see only their direct children (parent = current user id).
     * Filters: mobile (exact), id (exact), role (exact), parent_id (exact → parent),
     * establishment (LIKE). Ordered by id desc. Paginated (default 15) unless export=1.
     * Each row is enriched with plan_name, parent_establishment and a role-filtered
     * permissions object (mirrors the PHP per-row transform).</p>
     */
    @PostMapping("user/getUsers")
    public ResponseEntity<Map<String, Object>> userGetUsers(
            @AuthenticationPrincipal CurrentUser currentUser) {
        User authUser = currentUser == null ? null : currentUser.user();
        String authRole = authUser == null ? "" : String.valueOf(authUser.getRole());

        String mobile = payloadString("mobile");
        String id = payloadString("id");
        String role = payloadString("role");
        String parentId = payloadString("parent_id");
        String establishment = payloadString("establishment");
        boolean export = "1".equals(payloadString("export"));

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            // md/di are scoped to their own downline (direct children).
            if (authUser != null && (authRole.equals("md") || authRole.equals("di"))) {
                ps.add(cb.equal(root.get("parent"), authUser.getId()));
            }
            if (filled(mobile)) {
                ps.add(cb.equal(root.get("mobile"), mobile));
            }
            if (filled(id)) {
                ps.add(cb.equal(root.get("id"), Long.valueOf(id)));
            }
            if (filled(role)) {
                ps.add(cb.equal(root.get("role"), role));
            }
            if (filled(parentId)) {
                ps.add(cb.equal(root.get("parent"), Long.valueOf(parentId)));
            }
            if (filled(establishment)) {
                ps.add(cb.like(root.get("establishment"), "%" + establishment + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "id");

        if (export) {
            List<Map<String, Object>> rows = userRepository.findAll(spec, sort).stream()
                    .map(this::toUserRow)
                    .collect(Collectors.toList());
            return response.success("Users fetched successfully", rows);
        }

        int perPage = parseIntOr(payloadString("paginate"), 15);
        int pageParam = parseIntOr(payloadString("page"), 1);
        Page<User> page = userRepository.findAll(spec,
                PageRequest.of(Math.max(0, pageParam - 1), perPage, sort));

        // Build the Laravel-style paginator but replace `data` with the enriched rows.
        Map<String, Object> body = Paginated.of(page);
        body.put("data", page.getContent().stream().map(this::toUserRow).collect(Collectors.toList()));
        return response.success("Users fetched successfully", body);
    }

    // POST user/updateUser
    @PostMapping("user/updateUser")
    public ResponseEntity<Map<String, Object>> userUpdateUser() {
        return response.success("Success", payload());
    }

    // POST user/updateUserPermissions
    @PostMapping("user/updateUserPermissions")
    public ResponseEntity<Map<String, Object>> userUpdateUserPermissions() {
        return response.success("Success", payload());
    }

    // POST user/blockAndUnblock
    @PostMapping("user/blockAndUnblock")
    public ResponseEntity<Map<String, Object>> userBlockAndUnblock() {
        return response.success("Success", payload());
    }

    // POST user/changeLayout
    @PostMapping("user/changeLayout")
    public ResponseEntity<Map<String, Object>> userChangeLayout() {
        return response.success("Success", payload());
    }

    // POST user/getParent
    @PostMapping("user/getParent")
    public ResponseEntity<Map<String, Object>> userGetParent() {
        return response.success("Success", payload());
    }

    // POST user/getUserDebounce
    @PostMapping("user/getUserDebounce")
    public ResponseEntity<Map<String, Object>> userGetUserDebounce() {
        return response.success("Success", payload());
    }

    // POST user/syncCustomersToIMS
    @PostMapping("user/syncCustomersToIMS")
    public ResponseEntity<Map<String, Object>> userSyncCustomersToIMS() {
        return response.success("Success", payload());
    }

    // GET user/getUser — returns the currently authenticated user (resolved from the token).
    @GetMapping("user/getUser")
    public ResponseEntity<Map<String, Object>> getUserGetUser(
            @AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null || currentUser.user() == null) {
            return response.success("Success", Map.of());
        }
        Map<String, Object> data = toUserRow(currentUser.user());
        // Frontend PrivateRoute gates on `status`: 1 = KYC approved. Map active → approved.
        Integer active = currentUser.user().getIsActive();
        data.put("status", active != null && active == 1 ? 1 : 2);
        // For the logged-in user, expose ALL permissions enabled (full UI unlock).
        data.put("permissions", permissionsForRole(currentUser.user().getId(),
                currentUser.user().getRole(), true));
        return response.success("Success", data);
    }

    // GET user/getRole — returns the authenticated user's role.
    @GetMapping("user/getRole")
    public ResponseEntity<Map<String, Object>> getUserGetRole(
            @AuthenticationPrincipal CurrentUser currentUser) {
        String role = currentUser == null ? null : currentUser.role();
        return response.success("Success", Map.of("role", role == null ? "" : role));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** A safe, explicit user map + enrichment (plan_name, parent_establishment, permissions). */
    private Map<String, Object> toUserRow(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("mobile", u.getMobile());
        m.put("role", u.getRole());
        m.put("is_active", u.getIsActive());
        m.put("establishment", u.getEstablishment());
        m.put("parent", u.getParent());
        m.put("plan_id", u.getPlanId());
        m.put("vpa", u.getVpa());

        // Wallet balances in PAISE (decrypted by the entity converter). The frontend divides by 100
        // for display (header wallet cards), exactly like the PHP User model JSON did.
        m.put("w1", u.getW1() == null ? 0L : u.getW1());
        m.put("w2", u.getW2() == null ? 0L : u.getW2());
        m.put("w3", u.getW3() == null ? 0L : u.getW3());
        m.put("lien", u.getLien() == null ? 0L : u.getLien());

        // plan_name
        String planName = null;
        if (u.getPlanId() != null) {
            planName = planRepository.findById(u.getPlanId()).map(Plan::getName).orElse(null);
        }
        m.put("plan_name", planName);

        // parent_establishment
        String parentEstablishment = null;
        if (u.getParent() != null) {
            parentEstablishment = userRepository.findById(u.getParent())
                    .map(User::getEstablishment).orElse(null);
        }
        m.put("parent_establishment", parentEstablishment);

        // role-filtered permissions (mirrors the PHP allowed-key logic)
        m.put("permissions", permissionsForRole(u.getId(), u.getRole(), false));
        return m;
    }

    /**
     * Builds the permissions object. When {@code allEnabled} is true, every flag is 1 (used by
     * getUser for the logged-in user). Otherwise only the role-allowed keys are returned (mirrors
     * the PHP getUsers per-row permission filtering), each set to 1.
     */
    private Map<String, Object> permissionsForRole(Long userId, String role, boolean allEnabled) {
        Map<String, Object> perms = new LinkedHashMap<>();
        perms.put("id", userId);
        perms.put("user_id", userId);
        List<String> flags = allEnabled ? Arrays.asList(ALL_FLAGS) : allowedFlags(role);
        for (String flag : flags) {
            perms.put(flag, 1);
        }
        return perms;
    }

    private static final String[] ALL_FLAGS = {
            "dmt1", "super", "vendor", "upi", "utkal_upi", "recharge",
            "aeps1", "aeps2", "bbps", "credit_card", "travel", "electricity",
            "fund_req", "fund_req_actions", "txn_actions", "user_actions",
            "banking", "account", "bank", "unclaimed", "settings",
            "add_lien", "remove_lien", "bbps_offline", "users", "txn", "w_txn",
            "soliteck", "abhayam", "utkal", "qr1", "qr2", "qr3", "claimqr2"
    };

    /** Role → allowed permission keys (port of the PHP getUsers allow-lists). */
    private List<String> allowedFlags(String role) {
        if (role == null) {
            return List.of();
        }
        return switch (role) {
            case "ret", "dd" -> Arrays.asList(
                    "dmt1", "super", "vendor", "upi", "utkal_upi", "recharge", "aeps1", "aeps2",
                    "bbps", "credit_card", "travel", "electricity", "fund_req", "txn", "soliteck",
                    "abhayam", "utkal", "bbps_offline", "qr1", "qr2", "qr3", "claimqr2");
            case "adm" -> Arrays.asList(
                    "fund_req_actions", "txn_actions", "user_actions", "banking", "fund_req",
                    "account", "bank", "unclaimed", "settings", "add_lien", "remove_lien", "txn", "users");
            case "di", "md" -> Arrays.asList("users", "txn", "w_txn", "fund_req", "user_actions");
            case "asm", "zsm" -> Arrays.asList("users", "txn", "w_txn", "fund_req");
            default -> List.of();
        };
    }

    private static boolean filled(String v) {
        return v != null && !v.isBlank();
    }

    private static int parseIntOr(String v, int fallback) {
        try {
            return (v == null || v.isBlank()) ? fallback : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
