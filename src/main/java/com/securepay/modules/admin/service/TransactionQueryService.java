package com.securepay.modules.admin.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the combined transactions view by UNION-ing the per-service transaction tables
 * (aeps/dmt/payout/recharge), mirroring the legacy AdminController::getAllTxn. Supports the same
 * filters (user_id, txn_id, status, route, operator_name, service_name, date range), role-based
 * scoping (by a set of allowed user ids), ordering by created_at desc, and pagination/export.
 */
@Service
public class TransactionQueryService {

    @PersistenceContext
    private EntityManager em;

    /** A single service's SELECT with the normalized common column set. */
    private static String serviceSelect(String service, String table) {
        return "SELECT '" + service + "' AS service_name, txn_id, client_ref, user_id, "
                + "operator AS operator_name, operator_id, route, amount, "
                + "ret_comm, di_comm, md_comm, a_comm, charges, status, created_at, updated_at "
                + "FROM " + table;
    }

    /** aeps_txns has no a_comm column → substitute 0. */
    private static String aepsSelect() {
        return "SELECT 'AEPS' AS service_name, txn_id, client_ref, user_id, "
                + "operator AS operator_name, operator_id, route, amount, "
                + "ret_comm, di_comm, md_comm, 0 AS a_comm, charges, status, created_at, updated_at "
                + "FROM aeps_txns";
    }

    private static final String UNION_SQL =
            aepsSelect()
            + " UNION ALL " + serviceSelect("DMT", "dmt_txns")
            + " UNION ALL " + serviceSelect("VERIFICATION", "verification_txns")
            + " UNION ALL " + serviceSelect("PAYOUT", "payout_txns")
            + " UNION ALL " + serviceSelect("RECHARGE", "recharge_txns");

    public record Filters(
            List<Long> scopedUserIds, // null = no scoping (admin); non-null = restrict to these ids
            String userId,
            String txnId,
            String status,
            String route,
            String operatorName,
            String serviceName,
            String fromDate,
            String toDate
    ) {
    }

    /** Result: either a paginated map (Laravel shape) or, when export, a bare list. */
    public Map<String, Object> paginated(Filters f, int page, int perPage) {
        StringBuilder where = new StringBuilder();
        Map<String, Object> params = new LinkedHashMap<>();
        buildWhere(f, where, params);

        String base = "SELECT * FROM (" + UNION_SQL + ") all_txns" + where;
        String countSql = "SELECT COUNT(*) FROM (" + UNION_SQL + ") all_txns" + where;

        long total = ((Number) applyParams(em.createNativeQuery(countSql), params).getSingleResult()).longValue();

        Query q = applyParams(em.createNativeQuery(base + " ORDER BY created_at DESC"), params);
        q.setFirstResult(Math.max(0, page - 1) * perPage);
        q.setMaxResults(perPage);
        List<Map<String, Object>> rows = toRows(q.getResultList());

        int lastPage = (int) Math.ceil((double) total / perPage);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("current_page", page);
        body.put("data", rows);
        body.put("per_page", perPage);
        body.put("total", total);
        body.put("last_page", Math.max(lastPage, 1));
        body.put("from", rows.isEmpty() ? null : (page - 1) * perPage + 1);
        body.put("to", (page - 1) * perPage + rows.size());
        return body;
    }

    public List<Map<String, Object>> export(Filters f) {
        StringBuilder where = new StringBuilder();
        Map<String, Object> params = new LinkedHashMap<>();
        buildWhere(f, where, params);
        String base = "SELECT * FROM (" + UNION_SQL + ") all_txns" + where + " ORDER BY created_at DESC";
        return toRows(applyParams(em.createNativeQuery(base), params).getResultList());
    }

    private void buildWhere(Filters f, StringBuilder where, Map<String, Object> params) {
        List<String> clauses = new ArrayList<>();
        if (f.scopedUserIds() != null) {
            clauses.add("user_id IN (:scopedIds)");
            params.put("scopedIds", f.scopedUserIds().isEmpty() ? List.of(-1L) : f.scopedUserIds());
        }
        if (filled(f.userId())) {
            clauses.add("user_id = :userId");
            params.put("userId", Long.valueOf(f.userId()));
        }
        if (filled(f.txnId())) {
            clauses.add("txn_id = :txnId");
            params.put("txnId", f.txnId());
        }
        if (filled(f.status())) {
            clauses.add("status = :status");
            params.put("status", f.status().toUpperCase());
        }
        if (filled(f.route())) {
            clauses.add("route = :route");
            params.put("route", f.route().toUpperCase());
        }
        if (filled(f.operatorName())) {
            clauses.add("operator_name = :operatorName");
            params.put("operatorName", f.operatorName().toUpperCase());
        }
        if (filled(f.serviceName())) {
            clauses.add("service_name = :serviceName");
            params.put("serviceName", f.serviceName().replace('+', ' ').toUpperCase());
        }
        if (filled(f.fromDate()) && filled(f.toDate())) {
            clauses.add("created_at BETWEEN :fromDate AND :toDate");
            params.put("fromDate", f.fromDate() + " 00:00:00");
            params.put("toDate", f.toDate() + " 23:59:59");
        }
        if (!clauses.isEmpty()) {
            where.append(" WHERE ").append(String.join(" AND ", clauses));
        }
    }

    private Query applyParams(Query q, Map<String, Object> params) {
        params.forEach(q::setParameter);
        return q;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toRows(List<?> resultList) {
        // Native query with multiple selects returns Object[] per row; map to column names.
        String[] cols = {
                "service_name", "txn_id", "client_ref", "user_id", "operator_name", "operator_id",
                "route", "amount", "ret_comm", "di_comm", "md_comm", "a_comm", "charges", "status",
                "created_at", "updated_at"
        };
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object r : resultList) {
            Object[] arr = (Object[]) r;
            Map<String, Object> m = new LinkedHashMap<>();
            for (int i = 0; i < cols.length && i < arr.length; i++) {
                m.put(cols[i], arr[i]);
            }
            rows.add(m);
        }
        return rows;
    }

    /**
     * Aggregates service_summaries by user_id + service_name. {@code scopedUserId} non-null
     * restricts to that user (non-admin). Returns rows with user_name (establishment) attached.
     * Returns an empty list if the service_summaries table does not exist yet.
     */
    public List<Map<String, Object>> serviceSummary(
            Long scopedUserId, String serviceName, String fromDate, String toDate,
            String filterUserId,
            com.securepay.modules.user.repository.UserRepository userRepository) {

        List<String> clauses = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        if (scopedUserId != null) {
            clauses.add("user_id = :scoped");
            params.put("scoped", scopedUserId);
        }
        if (filled(serviceName)) {
            clauses.add("service_name = :svc");
            params.put("svc", serviceName.toUpperCase());
        }
        if (filled(fromDate) && filled(toDate)) {
            clauses.add("summary_date BETWEEN :fromD AND :toD");
            params.put("fromD", fromDate);
            params.put("toD", toDate);
        }
        if (filled(filterUserId)) {
            clauses.add("user_id = :fUser");
            params.put("fUser", Long.valueOf(filterUserId));
        }
        String where = clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);

        String sql = "SELECT user_id, service_name, "
                + "SUM(total_amount) AS total_amount, SUM(total_comm) AS total_comm, "
                + "SUM(total_tds) AS total_tds, SUM(total_charges) AS total_charges, "
                + "SUM(profit) AS total_profit "
                + "FROM service_summaries" + where
                + " GROUP BY user_id, service_name ORDER BY user_id, service_name";

        List<?> result;
        try {
            result = applyParams(em.createNativeQuery(sql), params).getResultList();
        } catch (Exception e) {
            // Table may not exist in this environment yet — return empty rather than 500.
            return new ArrayList<>();
        }

        String[] cols = {"user_id", "service_name", "total_amount", "total_comm",
                "total_tds", "total_charges", "total_profit"};
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object r : result) {
            Object[] arr = (Object[]) r;
            Map<String, Object> m = new LinkedHashMap<>();
            for (int i = 0; i < cols.length && i < arr.length; i++) {
                m.put(cols[i], arr[i]);
            }
            // user_name = establishment
            Object uid = m.get("user_id");
            String name = "Unknown";
            if (uid != null) {
                Long id = ((Number) uid).longValue();
                name = userRepository.findById(id)
                        .map(com.securepay.modules.user.entity.User::getEstablishment)
                        .orElse("Unknown");
            }
            m.put("user_name", name);
            rows.add(m);
        }
        return rows;
    }

    private static boolean filled(String v) {
        return v != null && !v.isBlank();
    }
}
