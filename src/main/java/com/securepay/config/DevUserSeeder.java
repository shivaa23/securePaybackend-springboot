package com.securepay.config;

import com.securepay.common.security.PasswordService;
import com.securepay.modules.account.entity.Account;
import com.securepay.modules.account.entity.AccountStatement;
import com.securepay.modules.account.repository.AccountRepository;
import com.securepay.modules.account.repository.AccountStatementRepository;
import com.securepay.modules.admin.entity.RouteEntity;
import com.securepay.modules.admin.repository.RouteRepository;
import com.securepay.modules.bank.entity.Bank;
import com.securepay.modules.bank.repository.BankRepository;
import com.securepay.modules.fundrequest.entity.FundRequest;
import com.securepay.modules.fundrequest.repository.FundRequestRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Seeds dummy users for local testing so the frontend can log in end-to-end.
 *
 * <p>GUARDED by {@code @Profile("local")} so it NEVER runs against dev/prod databases.
 * Idempotent: users are matched by mobile, so re-running only updates the existing rows.</p>
 *
 * <p>Login identity rule (AuthService): the username is {@code "TRANS" + user.id}. Ids are
 * DB-assigned (AUTO_INCREMENT), so the exact username is logged after each save.</p>
 *
 * Seeded users:
 * <ul>
 *   <li>Retailer — role "ret", mobile 9999999999 (username TRANS&lt;id&gt;)</li>
 *   <li>Admin    — role "adm", mobile 8888888888 (username TRANS&lt;id&gt;)</li>
 * </ul>
 * Both use password {@code Test@123} and MPIN {@code 123456}. Login returns a stateless JWT;
 * no token is stored in the database.
 */
@Component
@Order(1)
@Profile("local")
public class DevUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevUserSeeder.class);

    private static final String DEV_PASSWORD = "Test@123";
    private static final String DEV_MPIN = "123456";

    // Retailer
    private static final String RET_MOBILE = "9999999999";

    // Admin
    private static final String ADM_MOBILE = "8888888888";

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final AccountRepository accountRepository;
    private final AccountStatementRepository statementRepository;
    private final RouteRepository routeRepository;
    private final FundRequestRepository fundRequestRepository;
    private final BankRepository bankRepository;

    public DevUserSeeder(UserRepository userRepository, PasswordService passwordService,
                         AccountRepository accountRepository,
                         AccountStatementRepository statementRepository,
                         RouteRepository routeRepository,
                         FundRequestRepository fundRequestRepository,
                         BankRepository bankRepository) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.accountRepository = accountRepository;
        this.statementRepository = statementRepository;
        this.routeRepository = routeRepository;
        this.fundRequestRepository = fundRequestRepository;
        this.bankRepository = bankRepository;
    }

    @Override
    public void run(String... args) {
        User retailer = seedUser("Dev Retailer", "dev@securepay.local", RET_MOBILE, "ret");
        seedUser("Dev Admin", "admin@securepay.local", ADM_MOBILE, "adm");
        seedModuleDummyData(retailer);
    }

    /** One dummy row per Admin/Account-module table so every endpoint returns real data. */
    private void seedModuleDummyData(User retailer) {
        if (retailer == null) {
            return;
        }
        // --- Route (admin/getRoutes) ---
        if (routeRepository.count() == 0) {
            RouteEntity route = new RouteEntity();
            route.setName("Default Route");
            route.setCode("DEFAULT");
            route.setStatus(1);
            routeRepository.save(route);
            log.warn("[DevUserSeeder] Seeded 1 route (Default Route / DEFAULT)");
        }

        // --- Account (account/getAccounts) for the retailer ---
        // Only create the account if it is missing. NEVER overwrite an existing one: its balance and
        // credit_limit are live state (changed by approvals / statements) and must survive restarts.
        Account savedAccount = accountRepository.findByUserId(retailer.getId()).orElseGet(() -> {
            Account account = new Account();
            account.setUserId(retailer.getId());
            account.setName(retailer.getName());
            account.setEstablishment(retailer.getEstablishment() != null
                    ? retailer.getEstablishment() : "Dev Shop");
            account.setMobile(retailer.getMobile());
            account.setAsm(retailer.getParent());
            account.setCreditLimit(new BigDecimal("10000.00"));
            account.setBalance(new BigDecimal("5000.00"));
            account.setStatus(1);
            Account created = accountRepository.save(account);
            log.warn("[DevUserSeeder] Seeded account id={} for user {}", created.getId(), retailer.getId());
            return created;
        });

        // --- Account statement (accountStatement/getAccountStatement) ---
        if (statementRepository.findFirstByAccountIdOrderByIdDesc(savedAccount.getId()).isEmpty()) {
            AccountStatement st = new AccountStatement();
            st.setAccountId(savedAccount.getId());
            st.setRemarks(savedAccount.getEstablishment());
            st.setClaimedBy(retailer.getId());
            st.setCredit(new BigDecimal("5000.00"));
            st.setDebit(BigDecimal.ZERO);
            st.setBalance(new BigDecimal("5000.00"));
            st.setParticulars("Opening balance");
            statementRepository.save(st);
            log.warn("[DevUserSeeder] Seeded 1 account statement for account {}", savedAccount.getId());
        }

        // --- Bank (fundrequest/getFundRequestsSchema dropdown) ---
        if (bankRepository.count() == 0) {
            Bank bank = new Bank();
            bank.setName("HDFC Bank");
            bank.setAccountNumber("50100123456789");
            bank.setIfsc("HDFC0001234");
            bank.setStatus(1);
            bankRepository.save(bank);
            log.warn("[DevUserSeeder] Seeded 1 bank (HDFC Bank)");
        }

        // --- Fund request (fundrequest/getFundRequests, approve/reject/edit/delete) ---
        if (!fundRequestRepository.existsByClientRef("DEVFR-0001")) {
            FundRequest fr = new FundRequest();
            fr.setUserId(retailer.getId());
            fr.setUsername("TRANS" + retailer.getId());
            fr.setRole(retailer.getRole());
            fr.setName(savedAccount.getEstablishment());
            fr.setAmount(new BigDecimal("2500.00"));
            fr.setStatus("pending");
            fr.setBankName("HDFC Bank");
            fr.setMode("NEFT");
            fr.setBankRefId("REF" + System.currentTimeMillis());
            fr.setDate("2026-10-28");
            fr.setClientRef("DEVFR-0001");
            fr.setRemark("Dev top-up request");
            fundRequestRepository.save(fr);
            log.warn("[DevUserSeeder] Seeded 1 fund request (client_ref DEVFR-0001) for user {}", retailer.getId());
        }
    }

    /**
     * Creates or updates a dummy user, matched by mobile (idempotent). The login username is
     * derived from the DB-assigned id and logged along with the credentials.
     */
    private User seedUser(String name, String email, String mobile, String role) {
        Optional<User> existing = userRepository.findByMobile(mobile);
        User user = existing.orElseGet(User::new);

        user.setName(name);
        user.setEmail(email);
        user.setMobile(mobile);
        user.setRole(role);                // frontend routes by role (e.g. "adm" → /admin/dashboard)
        user.setIsActive(1);               // ActiveUserInterceptor requires is_active == 1
        user.setTwoFa("0");
        user.setWhitelistedIps(null);      // leave blank so IpWhitelistInterceptor doesn't restrict
        user.setPassword(passwordService.hashPassword(DEV_PASSWORD));  // bcrypt(password + pass-key)
        user.setMpin(passwordService.hashMpin(DEV_MPIN));              // bcrypt(mpin + pin-key)
        // Login now uses a stateless JWT — nothing stored in users.api_token.
        user.setApiToken(null);
        // Wallet columns (w1/w2/w3/lien) left NULL on purpose — AES-encrypted at rest; null == 0.

        User saved = userRepository.save(user);
        String username = "TRANS" + saved.getId();

        log.warn("[DevUserSeeder] =====================================================");
        log.warn("[DevUserSeeder]  {} dummy user (role={}) id={}",
                existing.isPresent() ? "Updated" : "Created", role, saved.getId());
        log.warn("[DevUserSeeder]  LOGIN USERNAME : {}", username);
        log.warn("[DevUserSeeder]  PASSWORD       : {}", DEV_PASSWORD);
        log.warn("[DevUserSeeder]  MPIN           : {}", DEV_MPIN);
        log.warn("[DevUserSeeder]  (token is a JWT issued at secureLogin; not stored in DB)");
        log.warn("[DevUserSeeder] =====================================================");
        return saved;
    }
}
