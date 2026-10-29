package com.securepay.modules.walletledger.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.util.Json;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Paginated;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import com.securepay.modules.wallet.entity.AbstractWalletLedger;
import com.securepay.modules.wallet.repository.WalletLedger2Repository;
import com.securepay.modules.wallet.repository.WalletLedger3Repository;
import com.securepay.modules.wallet.repository.WalletLedgerRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.Set;

/**
 * Port of {@code WalletLedger/Controllers}: W1 / W2 / W3 ledger listings, DI/MD own ledger and
 * the txn-id lookup across the service transaction tables.
 */
@RestController
public class WalletledgerController extends BaseController {

    /** txn_id prefix → table, in the exact order the source checks (first match wins). */
    private static final Map<String, String> TABLE_MAP = new LinkedHashMap<>();

    static {
        TABLE_MAP.put("PMT", "payout_txns");
        TABLE_MAP.put("DMT1", "dmt_txns");
        TABLE_MAP.put("DMT2", "dmt_txns");
        TABLE_MAP.put("ACV", "verification_txns");
        TABLE_MAP.put("AEPS", "aeps_txns");
        TABLE_MAP.put("W2W", "wallet_transactions");
        TABLE_MAP.put("W3W", "wallet_transactions");
        TABLE_MAP.put("AEPS2", "aeps_txns");
        TABLE_MAP.put("BBP", "bbps_txns");
        TABLE_MAP.put("RCH", "recharge_txns");
    }

    private final ResponseFactory response;
    private final WalletLedgerRepository ledger1;
    private final WalletLedger2Repository ledger2;
    private final WalletLedger3Repository ledger3;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;

    public WalletledgerController(ResponseFactory response, WalletLedgerRepository ledger1,
                                  WalletLedger2Repository ledger2, WalletLedger3Repository ledger3,
                                  UserRepository userRepository, JdbcTemplate jdbc) {
        this.response = response;
        this.ledger1 = ledger1;
        this.ledger2 = ledger2;
        this.ledger3 = ledger3;
        this.userRepository = userRepository;
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ helpers
    private static boolean filled(Map<String, Object> p, String k) {
        return p.get(k) != null && !p.get(k).toString().isBlank();
    }

    private static String s(Map<String, Object> p, String k) {
        return p.get(k) == null ? null : p.get(k).toString();
    }

    private List<Long> selfAndDescendants(Long rootId) {
        List<Long> ids = new ArrayList<>(List.of(rootId));
        Set<Long> seen = new LinkedHashSet<>(ids);
        Deque<Long> queue = new ArrayDeque<>(ids);
        while (!queue.isEmpty()) {
            for (User c : userRepository.findByParent(queue.poll())) {
                if (seen.add(c.getId())) {
                    ids.add(c.getId());
                    queue.add(c.getId());
                }
            }
        }
        return ids;
    }

    /** user_id / txn_id / service_name / from_date+to_date filters shared by all listings. */
    private <T extends AbstractWalletLedger> Specification<T> filters(Map<String, Object> p, List<Long> scope,
                                                                       Long exactUser, boolean userIdFilter,
                                                                       boolean txnFilter) {
        return (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (scope != null) {
                ps.add(root.get("userId").in(scope));
            }
            if (exactUser != null) {
                ps.add(cb.equal(root.get("userId"), exactUser));
            }
            if (userIdFilter && filled(p, "user_id")) {
                ps.add(cb.equal(root.get("userId"), Long.valueOf(s(p, "user_id").trim())));
            }
            if (txnFilter && filled(p, "txn_id")) {
                ps.add(cb.equal(root.get("txnId"), s(p, "txn_id")));
            }
            if (filled(p, "service_name")) {
                ps.add(cb.equal(root.get("service"), s(p, "service_name")));
            }
            if (filled(p, "from_date") && filled(p, "to_date")) {
                ps.add(cb.between(root.get("createdAt"),
                        LocalDate.parse(s(p, "from_date").trim()).atStartOfDay(),
                        LocalDate.parse(s(p, "to_date").trim()).atTime(LocalTime.MAX)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /** {@code row->establishment = row->user->establishment ?? 'Unknown'} */
    private Map<String, Object> withEstablishment(AbstractWalletLedger row) {
        Map<String, Object> m = Json.readMap(Json.write(row));
        String est = row.getUserId() == null ? null
                : userRepository.findById(row.getUserId()).map(User::getEstablishment).orElse(null);
        m.put("establishment", est == null ? "Unknown" : est);
        return m;
    }

    private <T extends AbstractWalletLedger> ResponseEntity<Map<String, Object>> list(
            JpaSpecificationExecutor<T> repo, Specification<T> spec, Map<String, Object> p, boolean allowExport) {
        Sort byIdDesc = Sort.by(Sort.Direction.DESC, "id");
        boolean export = allowExport && filled(p, "export") && "1".equals(s(p, "export"));
        if (export) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (T t : repo.findAll(spec, byIdDesc)) {
                rows.add(withEstablishment(t));
            }
            return rows.isEmpty() ? response.success("No Data Found", List.of())
                    : response.success("Wallet Ledgers fetched", rows);
        }
        int perPage = 15;
        int page = 1;
        try {
            if (filled(p, "paginate")) {
                perPage = Math.max(1, Integer.parseInt(s(p, "paginate").trim()));
            }
            if (filled(p, "page")) {
                page = Math.max(1, Integer.parseInt(s(p, "page").trim()));
            }
        } catch (NumberFormatException ignored) {
            // keep defaults
        }
        Page<T> result = repo.findAll(spec, PageRequest.of(page - 1, perPage, byIdDesc));
        if (result.getContent().isEmpty()) {
            return response.success("No Data Found", List.of());
        }
        Map<String, Object> body = Paginated.of(result);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (T t : result.getContent()) {
            rows.add(withEstablishment(t));
        }
        body.put("data", rows);
        return response.success("Wallet Ledgers fetched", body);
    }

    private static boolean isAdmin(User u) {
        return "sadm".equals(u.getRole()) || "adm".equals(u.getRole());
    }

    // ------------------------------------------------------------------ W1 ledger
    // POST walletledger/getWalletLedgers
    @PostMapping("walletledger/getWalletLedgers")
    public ResponseEntity<Map<String, Object>> getWalletLedgers(@AuthenticationPrincipal CurrentUser cu) {
        User user = cu.user();
        Map<String, Object> p = payload();
        List<Long> scope = isAdmin(user) ? null : selfAndDescendants(user.getId());
        return list(ledger1, filters(p, scope, null, true, true), p, true);
    }

    // ------------------------------------------------------------------ W2 ledger
    // POST walletledger/getWalletLedger2
    @PostMapping("walletledger/getWalletLedger2")
    public ResponseEntity<Map<String, Object>> getWalletLedger2(@AuthenticationPrincipal CurrentUser cu) {
        User user = cu.user();
        Map<String, Object> p = payload();
        List<Long> scope = isAdmin(user) ? null : selfAndDescendants(user.getId());
        return list(ledger2, filters(p, scope, null, true, true), p, true);
    }

    // ------------------------------------------------------------------ W3 ledger
    // POST walletledger/getWalletLedger3 — non-admins see only their own rows; admin may filter by
    // user_id (the source strips the 5-char "TRANS" prefix) or by mobile
    @PostMapping("walletledger/getWalletLedger3")
    public ResponseEntity<Map<String, Object>> getWalletLedger3(@AuthenticationPrincipal CurrentUser cu) {
        User user = cu.user();
        Map<String, Object> p = payload();
        Long exactUser = null;
        if (!isAdmin(user)) {
            exactUser = user.getId();
        } else {
            if (filled(p, "user_id")) {
                String raw = s(p, "user_id");
                String digits = raw.length() > 5 ? raw.substring(5) : "";
                exactUser = digits.matches("\\d+") ? Long.valueOf(digits) : -1L;
            }
            if (filled(p, "mobile")) {
                User u = userRepository.findFirstByMobile(s(p, "mobile")).orElse(null);
                if (u == null) {
                    return response.success("No Data Found", List.of());
                }
                exactUser = u.getId();
            }
        }
        return list(ledger3, filters(p, null, exactUser, false, false), p, false);
    }

    // POST walletledger/getWalletLedgersDi — DI / MD own ledger
    @PostMapping("walletledger/getWalletLedgersDi")
    public ResponseEntity<Map<String, Object>> getWalletLedgersDi(@AuthenticationPrincipal CurrentUser cu) {
        User user = cu.user();
        if (!"di".equals(user.getRole()) && !"md".equals(user.getRole())) {
            return response.error("Unauthorized Access", List.of(), HttpStatus.UNAUTHORIZED);
        }
        Map<String, Object> p = payload();
        return list(ledger1, filters(p, null, user.getId(), false, true), p, true);
    }

    // POST walletledger/getTxnData
    @PostMapping("walletledger/getTxnData")
    public ResponseEntity<Map<String, Object>> getTxnData() {
        String txnId = s(payload(), "txn_id");
        if (txnId == null || txnId.isBlank()) {
            return response.error("txn_id is required", List.of(), HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> result = null;
        for (Map.Entry<String, String> e : TABLE_MAP.entrySet()) {
            if (txnId.startsWith(e.getKey())) {
                try {
                    List<Map<String, Object>> rows = jdbc.queryForList(
                            "select * from " + e.getValue() + " where txn_id = ? limit 1", txnId);
                    if (!rows.isEmpty()) {
                        result = rows.get(0);
                    }
                } catch (DataAccessException ex) {
                    // table not present in this schema (e.g. bbps_txns) → behaves as "not found"
                    result = null;
                }
                break; // stop after the first matching prefix, as the source does
            }
        }
        if (result == null || result.isEmpty()) {
            return response.error("Transaction not found", List.of(), HttpStatus.NOT_FOUND);
        }
        return response.success("Transaction fetched", result);
    }

    // POST walletledger/getDebitWallateTxn — the route exists but the PHP controller has no such method
    // (the source answers with a server error); kept as an explicit 500.
    @PostMapping("walletledger/getDebitWallateTxn")
    public ResponseEntity<Map<String, Object>> getDebitWallateTxn() {
        return response.error("Server Error", "getDebitWallateTxn is not implemented in the source",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
