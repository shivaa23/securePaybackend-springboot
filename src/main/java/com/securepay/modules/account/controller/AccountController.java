package com.securepay.modules.account.controller;

import com.securepay.common.response.ResponseFactory;
import com.securepay.common.security.CurrentUser;
import com.securepay.common.web.BaseController;
import com.securepay.common.web.Paginated;
import com.securepay.modules.account.entity.Account;
import com.securepay.modules.account.entity.AccountStatement;
import com.securepay.modules.account.repository.AccountRepository;
import com.securepay.modules.account.repository.AccountStatementRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AccountController — ports the Account module (accounts + account statements) from
 * app/Modules/Account. Covers CRUD, schemas, deleteLastTxn, and statement CRUD with the
 * legacy role checks, filters and balance side-effects.
 */
@RestController
public class AccountController extends BaseController {

    private final ResponseFactory response;
    private final AccountRepository accountRepository;
    private final AccountStatementRepository statementRepository;
    private final UserRepository userRepository;

    private static final Set<String> ADMIN_ROLES = Set.of("adm", "sadm");

    public AccountController(ResponseFactory response, AccountRepository accountRepository,
                             AccountStatementRepository statementRepository,
                             UserRepository userRepository) {
        this.response = response;
        this.accountRepository = accountRepository;
        this.statementRepository = statementRepository;
        this.userRepository = userRepository;
    }

    // ================================================================
    // Accounts
    // ================================================================

    // POST account/getAccounts — active accounts with optional filters (adm/sadm only).
    @PostMapping("account/getAccounts")
    public ResponseEntity<Map<String, Object>> accountGetAccounts(
            @AuthenticationPrincipal CurrentUser currentUser) {
        if (!isAdmin(currentUser)) {
            return response.error("Unauthorised Action", List.of(), HttpStatus.FORBIDDEN);
        }
        String userId = payloadString("user_id");
        String name = payloadString("name");
        String mobile = payloadString("mobile");
        String establishment = payloadString("establishment");

        Specification<Account> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("status"), 1));
            if (filled(userId)) ps.add(cb.equal(root.get("userId"), Long.valueOf(userId)));
            if (filled(name)) ps.add(cb.equal(root.get("name"), name));
            if (filled(mobile)) ps.add(cb.equal(root.get("mobile"), mobile));
            if (filled(establishment)) ps.add(cb.equal(root.get("establishment"), establishment));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        List<Map<String, Object>> accounts = accountRepository
                .findAll(spec, Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(this::accountRow)
                .collect(Collectors.toList());
        return response.success("User accounts fetched successfully", accounts);
    }

    /** Explicit account map (avoids entity serialization surprises during response encryption). */
    private Map<String, Object> accountRow(Account a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("user_id", a.getUserId());
        m.put("name", a.getName());
        m.put("establishment", a.getEstablishment());
        m.put("mobile", a.getMobile());
        m.put("asm", a.getAsm());
        m.put("type", a.getType());
        m.put("credit_limit", a.getCreditLimit());
        m.put("balance", a.getBalance());
        m.put("status", a.getStatus());
        m.put("created_at", a.getCreatedAt());
        m.put("updated_at", a.getUpdatedAt());
        return m;
    }

    // POST account/createAccount — adm/sadm only; one account per user.
    @PostMapping("account/createAccount")
    public ResponseEntity<Map<String, Object>> accountCreateAccount(
            @AuthenticationPrincipal CurrentUser currentUser) {
        if (!isAdmin(currentUser)) {
            return response.error("Unauthorised Action", List.of(), HttpStatus.FORBIDDEN);
        }
        String userIdStr = payloadString("user_id");
        if (!filled(userIdStr)) {
            return response.error("Validation Failed", Map.of("user_id", "user_id is required"),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Long userId = Long.valueOf(userIdStr);
        if (accountRepository.findByUserId(userId).isPresent()) {
            return response.error("Account already exists", List.of(), HttpStatus.FORBIDDEN);
        }
        User user = userRepository.findById(userId).orElse(null);

        Account account = new Account();
        account.setUserId(userId);
        account.setName(orElse(payloadString("name"), user != null ? user.getName() : null));
        account.setEstablishment(orElse(payloadString("establishment"),
                user != null ? user.getEstablishment() : null));
        account.setMobile(orElse(payloadString("mobile"), user != null ? user.getMobile() : null));
        account.setAsm(user != null ? user.getParent() : null);
        account.setCreditLimit(decimalOr(payloadString("credit_limit"), null));
        account.setBalance(decimalOr(payloadString("balance"), BigDecimal.ZERO));
        account.setStatus(1);

        Account saved = accountRepository.save(account);
        return response.success("Account Created Successfully", saved);
    }

    // POST account/updateAccount — adm/sadm only; updates credit_limit/status.
    @PostMapping("account/updateAccount")
    public ResponseEntity<Map<String, Object>> accountUpdateAccount(
            @AuthenticationPrincipal CurrentUser currentUser) {
        String idStr = payloadString("id");
        if (!filled(idStr)) {
            return response.error("Validation Error", Map.of("id", "id is required"),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!isAdmin(currentUser)) {
            return response.error("Unauthorised Action", List.of(), HttpStatus.FORBIDDEN);
        }
        Account account = accountRepository.findById(Long.valueOf(idStr)).orElse(null);
        if (account == null) {
            return response.error("Account not found", "", HttpStatus.FORBIDDEN);
        }
        BigDecimal creditLimit = decimalOr(payloadString("credit_limit"), account.getCreditLimit());
        account.setCreditLimit(creditLimit);
        String status = payloadString("status");
        if (filled(status)) account.setStatus(Integer.valueOf(status));
        return response.success("Account updated Successfully", accountRepository.save(account));
    }

    // POST account/deleteAccount
    @PostMapping("account/deleteAccount")
    public ResponseEntity<Map<String, Object>> accountDeleteAccount(
            @AuthenticationPrincipal CurrentUser currentUser) {
        String idStr = payloadString("id");
        if (!filled(idStr)) {
            return response.error("Validation Failed", Map.of("id", "id is required"),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Optional<Account> acc = accountRepository.findById(Long.valueOf(idStr));
        if (acc.isEmpty()) {
            return response.error("Account could not be deleted", List.of(), HttpStatus.FORBIDDEN);
        }
        accountRepository.delete(acc.get());
        return response.success("Account deleted successfully", List.of());
    }

    // POST account/getAccountSchema — form schema with dd/ret/di/md/api users.
    @PostMapping("account/getAccountSchema")
    public ResponseEntity<Map<String, Object>> accountGetAccountSchema() {
        return response.success("Schema fetchSuccessfully", accountSchema());
    }

    // POST account/getAccounOthertSchema — form schema with asm users.
    @PostMapping("account/getAccounOthertSchema")
    public ResponseEntity<Map<String, Object>> accountGetAccounOthertSchema() {
        return response.success("Schema fetchSuccessfully", accountOthersSchema());
    }

    // POST account/deleteLastTxn — sadm only; reverts balance and deletes the latest statement.
    @PostMapping("account/deleteLastTxn")
    @Transactional
    public ResponseEntity<Map<String, Object>> accountDeleteLastTxn(
            @AuthenticationPrincipal CurrentUser currentUser) {
        String accountId = payloadString("account_id");
        if (!filled(accountId)) {
            return response.error("Validation Error", Map.of("account_id", "account_id is required"),
                    HttpStatus.FORBIDDEN);
        }
        String role = roleOf(currentUser);
        if (!"sadm".equals(role)) {
            return response.error("Unauthorised Access", "", HttpStatus.FORBIDDEN);
        }
        AccountStatement last = statementRepository
                .findFirstByAccountIdOrderByIdDesc(Long.valueOf(accountId)).orElse(null);
        if (last == null) {
            return response.error("No Transaction Found To Delete", "", HttpStatus.FORBIDDEN);
        }
        Account account = accountRepository.findById(last.getAccountId()).orElse(null);
        if (account == null) {
            return response.error("Account not found", "", HttpStatus.NOT_FOUND);
        }
        BigDecimal credit = nz(last.getCredit());
        BigDecimal debit = nz(last.getDebit());
        // Reverse the effect of the deleted entry: subtract its credit, add back its debit.
        account.setBalance(nz(account.getBalance()).subtract(credit).add(debit));
        accountRepository.save(account);
        statementRepository.delete(last);
        return response.success("Transaction deleted successfully", "");
    }

    // POST account/encryptPayload — utility passthrough (kept for route parity).
    @PostMapping("account/encryptPayload")
    public ResponseEntity<Map<String, Object>> accountEncryptPayload() {
        return response.success("Success", payload());
    }

    // ================================================================
    // Account Statements
    // ================================================================

    // POST accountStatement/getAccountStatement — paginated statements for an account (adm/sadm).
    @PostMapping("accountStatement/getAccountStatement")
    public ResponseEntity<Map<String, Object>> accountStatementGet(
            @AuthenticationPrincipal CurrentUser currentUser) {
        if (!isAdmin(currentUser)) {
            return response.error("Unauthorised Action", List.of(), HttpStatus.FORBIDDEN);
        }
        String accountId = payloadString("account_id");
        if (!filled(accountId)) {
            return response.error("Validation Failed", Map.of("account_id", "account_id is required"),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        var page = statementRepository.findByAccountIdOrderByIdDesc(
                Long.valueOf(accountId), PageRequest.of(0, 10));
        return response.success("Account statements fetched successfully", Paginated.of(page));
    }

    // POST accountStatement/createAccountStatement — adds a ledger entry + updates balance.
    @PostMapping("accountStatement/createAccountStatement")
    @Transactional
    public ResponseEntity<Map<String, Object>> accountStatementCreate(
            @AuthenticationPrincipal CurrentUser currentUser) {
        if (!isAdmin(currentUser)) {
            return response.error("Unauthorised Action", List.of(), HttpStatus.FORBIDDEN);
        }
        String accountId = payloadString("account_id");
        if (!filled(accountId)) {
            return response.error("Validation Failed", Map.of("account_id", "account_id is required"),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Account account = accountRepository.findById(Long.valueOf(accountId)).orElse(null);
        if (account == null) {
            return response.error("Account not found", List.of(), HttpStatus.NOT_FOUND);
        }
        User userData = account.getUserId() == null ? null
                : userRepository.findById(account.getUserId()).orElse(null);

        BigDecimal credit = decimalOr(payloadString("credit"), BigDecimal.ZERO);
        BigDecimal debit = decimalOr(payloadString("debit"), BigDecimal.ZERO);
        BigDecimal balance = nz(account.getBalance());
        if (credit.signum() > 0) {
            balance = balance.add(credit);
        } else if (debit.signum() > 0) {
            if (balance.compareTo(debit) < 0) {
                return response.error("Insufficient balance", List.of(), HttpStatus.BAD_REQUEST);
            }
            balance = balance.subtract(debit);
        }
        account.setBalance(balance);
        accountRepository.save(account);

        AccountStatement st = new AccountStatement();
        st.setAccountId(account.getId());
        st.setRemarks(userData != null ? userData.getEstablishment() : null);
        st.setBankId(longOrNull(payloadString("bank_id")));
        st.setClaimedBy(currentUser != null && currentUser.user() != null ? currentUser.user().getId() : null);
        st.setCredit(credit);
        st.setDebit(debit);
        st.setBalance(balance);
        st.setParticulars(payloadString("particulars"));
        st.setBankTxnid(longOrNull(payloadString("id")));
        AccountStatement saved = statementRepository.save(st);
        return response.success("Account statement created successfully", saved);
    }

    // POST accountStatement/updateAccountStatement
    @PostMapping("accountStatement/updateAccountStatement")
    public ResponseEntity<Map<String, Object>> accountStatementUpdate(
            @AuthenticationPrincipal CurrentUser currentUser) {
        String idStr = payloadString("id");
        if (!filled(idStr)) {
            return response.error("Validation Error", Map.of("id", "id is required"),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!isAdmin(currentUser)) {
            return response.error("Unauthorised Action", List.of(), HttpStatus.FORBIDDEN);
        }
        AccountStatement st = statementRepository.findById(Long.valueOf(idStr)).orElse(null);
        if (st == null) {
            return response.error("Account statement not found", "", HttpStatus.NOT_FOUND);
        }
        if (filled(payloadString("account_id"))) st.setAccountId(Long.valueOf(payloadString("account_id")));
        if (filled(payloadString("remarks"))) st.setRemarks(payloadString("remarks"));
        if (filled(payloadString("bank_id"))) st.setBankId(Long.valueOf(payloadString("bank_id")));
        if (filled(payloadString("credit"))) st.setCredit(new BigDecimal(payloadString("credit")));
        if (filled(payloadString("debit"))) st.setDebit(new BigDecimal(payloadString("debit")));
        if (filled(payloadString("balance"))) st.setBalance(new BigDecimal(payloadString("balance")));
        if (filled(payloadString("particulars"))) st.setParticulars(payloadString("particulars"));
        return response.success("Account statement updated successfully", statementRepository.save(st));
    }

    // POST accountStatement/deleteAccountStatement
    @PostMapping("accountStatement/deleteAccountStatement")
    public ResponseEntity<Map<String, Object>> accountStatementDelete() {
        String idStr = payloadString("id");
        if (!filled(idStr)) {
            return response.error("Validation Failed", Map.of("id", "id is required"),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Optional<AccountStatement> st = statementRepository.findById(Long.valueOf(idStr));
        if (st.isEmpty()) {
            return response.error("Account statement not found", List.of(), HttpStatus.NOT_FOUND);
        }
        statementRepository.delete(st.get());
        return response.success("Account statement deleted successfully", List.of());
    }

    // POST accountStatement/getAccountStatementSchema
    @PostMapping("accountStatement/getAccountStatementSchema")
    public ResponseEntity<Map<String, Object>> accountStatementSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("formName", "Account Statement Schema");
        schema.put("fields", List.of(
                field("account_id", "Account", "number", true),
                field("credit", "Credit", "number", false),
                field("debit", "Debit", "number", false),
                field("particulars", "Particulars", "text", false)));
        return response.success("Schema fetched successfully", schema);
    }

    // POST accountStatement/encryptPayload — utility passthrough (route parity).
    @PostMapping("accountStatement/encryptPayload")
    public ResponseEntity<Map<String, Object>> accountStatementEncryptPayload() {
        return response.success("Success", payload());
    }

    // ================================================================
    // Schema helpers (mirror AccountValidation::getAccountSchema / getAccountOthersSchema)
    // ================================================================

    private Map<String, Object> accountSchema() {
        List<Map<String, Object>> users = usersForRoles(List.of("dd", "ret", "di", "md", "api"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("formName", "Account Schema");
        schema.put("fields", List.of(
                Map.of("name", "user_id", "label", "Select User", "type", "autocomplete",
                        "placeholder", "Choose a user", "options", users, "required", true),
                Map.of("name", "credit_limit", "label", "Credit Limit", "type", "number",
                        "placeholder", "Enter credit limit (optional)", "required", false),
                Map.of("name", "status", "label", "Status", "type", "dropdown",
                        "placeholder", "Select status", "required", false,
                        "options", List.of(
                                Map.of("label", "Inactive", "value", 0),
                                Map.of("label", "Active", "value", 1)))));
        return schema;
    }

    private Map<String, Object> accountOthersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("formName", "Account Schema");
        schema.put("fields", List.of(
                field("name", "Owner name", "text", true),
                field("establishment", "Shop name", "text", true),
                field("mobile", "Registered mobile", "text", true),
                field("credit_limit", "Credit Limit", "number", false)));
        return schema;
    }

    private List<Map<String, Object>> usersForRoles(List<String> roles) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && roles.contains(u.getRole()))
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", u.getName() + " (" + u.getMobile() + ")");
                    m.put("id", u.getId());
                    m.put("mobile", u.getMobile());
                    m.put("establishment", u.getEstablishment());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private static Map<String, Object> field(String name, String label, String type, boolean required) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("label", label);
        f.put("type", type);
        f.put("required", required);
        return f;
    }

    // ------------------------------------------------------------------
    // small utils
    // ------------------------------------------------------------------

    private boolean isAdmin(CurrentUser cu) {
        return ADMIN_ROLES.contains(roleOf(cu));
    }

    private String roleOf(CurrentUser cu) {
        return cu == null || cu.user() == null ? "" : String.valueOf(cu.user().getRole());
    }

    private static boolean filled(String v) {
        return v != null && !v.isBlank();
    }

    private static String orElse(String v, String fallback) {
        return filled(v) ? v : fallback;
    }

    private static BigDecimal decimalOr(String v, BigDecimal fallback) {
        try {
            return filled(v) ? new BigDecimal(v.trim()) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Long longOrNull(String v) {
        try {
            return filled(v) ? Long.valueOf(v.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
