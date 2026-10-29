package com.securepay.modules.admin.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.modules.admin.entity.RouteEntity;
import com.securepay.modules.admin.repository.RouteRepository;
import com.securepay.modules.admin.service.TransactionQueryService;
import com.securepay.modules.admin.service.TransactionQueryService.Filters;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AdminController — ports the admin module endpoints at their exact paths (doc 05).
 */
@RestController
public class AdminController extends BaseController {

    private final ResponseFactory response;
    private final TransactionQueryService transactionQueryService;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;

    private static final Set<String> ADMIN_ROLES = Set.of("adm", "sadm");

    public AdminController(ResponseFactory response, TransactionQueryService transactionQueryService,
                           UserRepository userRepository, RouteRepository routeRepository) {
        this.response = response;
        this.transactionQueryService = transactionQueryService;
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
    }

    // POST admin/getRoutes — active routes (status = 1) with name + code.
    @PostMapping("admin/getRoutes")
    public ResponseEntity<Map<String, Object>> adminGetRoutes() {
        List<Map<String, Object>> routes = routeRepository.findByStatusOrderByIdAsc(1).stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", r.getName());
                    m.put("code", r.getCode());
                    return m;
                })
                .collect(Collectors.toList());
        return response.success("Routes fetched", routes);
    }

    // POST admin/getAllTxn — legacy route name.
    @PostMapping("admin/getAllTxn")
    public ResponseEntity<Map<String, Object>> adminGetAllTxn(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return transactions(currentUser);
    }

    // POST admin/getTransactions — the path the frontend transactions page calls.
    @PostMapping("admin/getTransactions")
    public ResponseEntity<Map<String, Object>> adminGetTransactions(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return transactions(currentUser);
    }

    /**
     * POST admin/userCommssionReport — aggregates {@code service_summaries} by user_id + service.
     * Port of AdminController::userCommssionReport: admins see all; others are scoped to self.
     * Filters: service_name, from_date/to_date (summary_date range), user_id (adm/md/sadm only).
     * Returns {@code {data:[...], totals:{...}}} with user_name (establishment) per row.
     */
    @PostMapping("admin/userCommssionReport")
    public ResponseEntity<Map<String, Object>> adminUserCommssionReport(
            @AuthenticationPrincipal CurrentUser currentUser) {
        User authUser = currentUser == null ? null : currentUser.user();
        String role = authUser == null ? "" : String.valueOf(authUser.getRole());
        boolean isAdmin = ADMIN_ROLES.contains(role);

        List<Map<String, Object>> rows = transactionQueryService.serviceSummary(
                isAdmin ? null : (authUser == null ? -1L : authUser.getId()),
                payloadString("service_name"),
                payloadString("from_date"),
                payloadString("to_date"),
                (isAdmin || role.equals("md")) ? payloadString("user_id") : null,
                userRepository);

        // Grand totals across rows.
        double tAmount = 0, tComm = 0, tTds = 0, tCharges = 0, tProfit = 0;
        for (Map<String, Object> r : rows) {
            tAmount += num(r.get("total_amount"));
            tComm += num(r.get("total_comm"));
            tTds += num(r.get("total_tds"));
            tCharges += num(r.get("total_charges"));
            tProfit += num(r.get("total_profit"));
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("total_amount", tAmount);
        totals.put("total_comm", tComm);
        totals.put("total_tds", tTds);
        totals.put("total_charges", tCharges);
        totals.put("total_profit", tProfit);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("data", rows);
        data.put("totals", totals);
        return response.success("Service summary fetched successfully", data);
    }

    private static double num(Object o) {
        return o == null ? 0 : ((Number) o).doubleValue();
    }

    // ------------------------------------------------------------------

    /**
     * Combined transactions view (UNION across aeps/dmt/payout/recharge) with the legacy filters,
     * role scoping and pagination/export. Mirrors AdminController::getAllTxn.
     */
    private ResponseEntity<Map<String, Object>> transactions(CurrentUser currentUser) {
        User authUser = currentUser == null ? null : currentUser.user();
        String role = authUser == null ? "" : String.valueOf(authUser.getRole());

        // Role scoping: admins see all (null). md/di see self + downline. Others see self only.
        List<Long> scopedIds;
        if (ADMIN_ROLES.contains(role)) {
            scopedIds = null;
        } else if (authUser != null && (role.equals("md") || role.equals("di"))) {
            scopedIds = downlineIds(authUser.getId(), role);
        } else if (authUser != null) {
            scopedIds = List.of(authUser.getId());
        } else {
            scopedIds = List.of(-1L);
        }

        Filters f = new Filters(
                scopedIds,
                payloadString("user_id"),
                payloadString("txn_id"),
                payloadString("status"),
                payloadString("route"),
                payloadString("operator_name"),
                payloadString("service_name"),
                payloadString("from_date"),
                payloadString("to_date"));

        if ("1".equals(payloadString("export"))) {
            List<Map<String, Object>> rows = transactionQueryService.export(f);
            // Return under the same envelope; data is a bare list for export.
            return response.success("Transactions fetched successfully", Map.of("data", rows));
        }

        int perPage = parseIntOr(payloadString("paginate"), 15);
        int page = parseIntOr(payloadString("page"), 1);
        return response.success("Transactions fetched successfully",
                transactionQueryService.paginated(f, page, perPage));
    }

    /** self + direct children (md: + grandchildren) — approximates the legacy downline ids. */
    private List<Long> downlineIds(Long rootId, String role) {
        List<Long> ids = new ArrayList<>();
        ids.add(rootId);
        List<User> children = userRepository.findByParent(rootId);
        for (User c : children) {
            ids.add(c.getId());
            if (role.equals("md")) {
                for (User g : userRepository.findByParent(c.getId())) {
                    ids.add(g.getId());
                }
            }
        }
        return ids;
    }

    private static int parseIntOr(String v, int fallback) {
        try {
            return (v == null || v.isBlank()) ? fallback : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
