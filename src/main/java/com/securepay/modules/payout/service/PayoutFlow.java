package com.securepay.modules.payout.service;

import com.securepay.common.audit.AuditLogService;
import com.securepay.common.exception.ApiException;
import com.securepay.common.util.Json;
import com.securepay.modules.auditlog.entity.AuditLog;
import com.securepay.modules.auth.service.OtpService;
import com.securepay.modules.commission.dto.CommissionResult;
import com.securepay.modules.commission.service.AdminCommissionCalculator;
import com.securepay.modules.commission.service.CommissionCalculator;
import com.securepay.modules.misc.service.UuidService;
import com.securepay.modules.payout.entity.PayoutSender;
import com.securepay.modules.payout.entity.PayoutTxn;
import com.securepay.modules.payout.repository.PayoutTxnRepository;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.service.repository.ServiceRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.wallet.service.WalletService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Shared two-phase payout pipeline used by payOut / payOut2 / payOutAbhayam / payOutUtkal / upiUtkal
 * (the PHP controllers all repeat the same sequence):
 *
 * <ol>
 *   <li>pre-checks (role, MPIN, 2-minute duplicate guard, sender, limit, service, commission, W1)</li>
 *   <li>phase 1, one committed transaction: txn row + W1 debit + audit log (INITIATED)</li>
 *   <li>provider call, <b>outside</b> any DB transaction</li>
 *   <li>phase 2, new transaction: response handler (status, refund, commissions) + audit update</li>
 * </ol>
 */
@Service
public class PayoutFlow {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final List<String> PAYOUT_ROLES = List.of("api", "dd", "ret");

    /** Provider call: returns the decoded provider response. */
    @FunctionalInterface
    public interface Dispatch {
        Map<String, Object> call(PayoutTxn txn, ServiceEntity service, User user, PayoutSender sender);
    }

    /** Response handler (one of {@link PayoutResponseHandler}'s methods). */
    @FunctionalInterface
    public interface Handler {
        ResponseEntity<Map<String, Object>> handle(PayoutTxn txn, Map<String, Object> resp, User user,
                                                   ServiceEntity service, PayoutSender sender);
    }

    /**
     * @param dupBeforeAuth     payOut checks the 2-minute duplicate before MPIN/role; the others after
     * @param senderFinder      mobile number to sender (null when missing/unverified)
     * @param routeAllowed      extra service-route guard (upiUtkal), or null
     * @param duplicateRefAs409 payOut maps a DB duplicate client_ref to HTTP 409
     */
    public record Spec(boolean dupBeforeAuth, Function<String, PayoutSender> senderFinder,
                       Predicate<ServiceEntity> routeAllowed, boolean duplicateRefAs409,
                       Dispatch dispatch, Handler handler) {
    }

    private final ServiceRepository serviceRepository;
    private final CommissionCalculator commissionCalculator;
    private final AdminCommissionCalculator adminCommissionCalculator;
    private final PayoutService payoutService;
    private final WalletService walletService;
    private final AuditLogService auditLogService;
    private final PayoutTxnRepository payoutTxnRepository;
    private final OtpService otpService;
    private final UuidService uuidService;
    private final TransactionTemplate tx;

    public PayoutFlow(ServiceRepository serviceRepository, CommissionCalculator commissionCalculator,
                      AdminCommissionCalculator adminCommissionCalculator, PayoutService payoutService,
                      WalletService walletService, AuditLogService auditLogService,
                      PayoutTxnRepository payoutTxnRepository, OtpService otpService, UuidService uuidService,
                      PlatformTransactionManager transactionManager) {
        this.serviceRepository = serviceRepository;
        this.commissionCalculator = commissionCalculator;
        this.adminCommissionCalculator = adminCommissionCalculator;
        this.payoutService = payoutService;
        this.walletService = walletService;
        this.auditLogService = auditLogService;
        this.payoutTxnRepository = payoutTxnRepository;
        this.otpService = otpService;
        this.uuidService = uuidService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** {@code Uuid::check}: a client_ref may be consumed once, else 402 "Duplicate Transaction". */
    public void consumeUuid(PayoutCtx c, String action) {
        if (!uuidService.consume(c.s("client_ref"), action, c.json())) {
            throw new ApiException("Duplicate Transaction", HttpStatus.PAYMENT_REQUIRED, List.of());
        }
    }

    private void requireRole(User user) {
        if (!PAYOUT_ROLES.contains(user.getRole())) {
            throw new ApiException("Action not Authorised", HttpStatus.FORBIDDEN, "");
        }
    }

    private void requireMpin(User user, PayoutCtx c) {
        if (!otpService.verifyMpin(user, c.s("mpin"))) {
            throw new ApiException("invalid mpin", HttpStatus.FORBIDDEN, List.of());
        }
    }

    private void requireNoRecentDuplicate(PayoutCtx c) {
        LocalDateTime since = LocalDateTime.now(IST).minusMinutes(2);
        if (payoutTxnRepository.existsByAccountNumberAndAmountAndStatusInAndCreatedAtGreaterThanEqual(
                c.s("account_number"), c.amount(), List.of("PENDING", "SUCCESS"), since)) {
            throw new ApiException("Transaction With Same Amount To This Account No Is Under Process "
                    + "Try After 2 Minutes Or Try Changing Amount", HttpStatus.TOO_MANY_REQUESTS, List.of());
        }
    }

    /** Runs pre-checks, phase 1, provider call and phase 2; see class doc. */
    public ResponseEntity<Map<String, Object>> run(User user, PayoutCtx c, Spec spec) {
        if (spec.dupBeforeAuth()) {
            requireNoRecentDuplicate(c);
            requireMpin(user, c);
            requireRole(user);
        } else {
            requireRole(user);
            requireMpin(user, c);
            requireNoRecentDuplicate(c);
        }
        PayoutSender sender = spec.senderFinder().apply(c.s("mobile_number"));
        if (sender == null) {
            throw new ApiException("Sender Not Found Or Unverified", HttpStatus.FORBIDDEN, "");
        }
        BigDecimal amount = c.amount();
        if (sender.getRemLimit() == null || sender.getRemLimit().compareTo(amount) < 0) {
            throw new ApiException("Sender Limit Exceeds", HttpStatus.FORBIDDEN, "");
        }
        Long operatorId = Long.valueOf(c.s("operator").trim());
        ServiceEntity service = serviceRepository.findById(operatorId)
                .orElseThrow(() -> new ApiException("Service not Configured", HttpStatus.FORBIDDEN, ""));
        if (spec.routeAllowed() != null && !spec.routeAllowed().test(service)) {
            throw new ApiException("Invalid Route", HttpStatus.FORBIDDEN, "");
        }
        CommissionResult comm = commissionCalculator.calculateCommission(user, service.getName(), amount);
        if (comm == null || comm.isEmpty()) {
            throw new ApiException("Commission Slab Not Configured", HttpStatus.FORBIDDEN, "");
        }
        AdminCommissionCalculator.AdminCommission aRaw =
                adminCommissionCalculator.calculateAdminCommission(service.getName(), amount, service.getRoute());
        if (aRaw == null || aRaw.isEmpty()) {
            throw new ApiException("Admin Commission Slab Not Configured", HttpStatus.FORBIDDEN, "");
        }
        comm = commissionCalculator.finalCommission(comm, amount, service);
        BigDecimal aComm = adminCommissionCalculator.finalCommission(aRaw, amount);

        // PHP: $user->w1 < ($amount + $user->lien) * 100 + ($comm['ret'] ?? $comm['dd']) * 100
        // (kept verbatim; lien is already paise, so with a non-zero lien this over-reserves.)
        BigDecimal lien = BigDecimal.valueOf(user.getLien() == null ? 0L : user.getLien());
        BigDecimal need = amount.add(lien).multiply(BigDecimal.valueOf(100))
                .add(PayoutService.charge(comm) == null ? BigDecimal.ZERO
                        : PayoutService.charge(comm).multiply(BigDecimal.valueOf(100)));
        BigDecimal w1 = BigDecimal.valueOf(user.getW1() == null ? 0L : user.getW1());
        if (w1.compareTo(need) < 0) {
            throw new ApiException("Insufficient W1 Balance", HttpStatus.FORBIDDEN, "");
        }

        // ---- phase 1 (committed): txn + debit + audit
        final CommissionResult commF = comm;
        record Init(PayoutTxn txn, AuditLog audit) {
        }
        Init init;
        try {
            init = tx.execute(status -> {
                PayoutTxn txn = payoutService.create(user, service, c, commF, aComm);
                walletService.debit(user, amount, txn, service);
                AuditLog audit = auditLogService.logRequest(user.getId(), user.getRole(), service.getName(),
                        c.json(), c.ip(), c.s("client_ref"), c.userAgent());
                return new Init(txn, audit);
            });
        } catch (DataIntegrityViolationException e) {
            if (spec.duplicateRefAs409()) {
                throw new ApiException("Duplicate client_ref detected", HttpStatus.CONFLICT, List.of());
            }
            throw new ApiException("Transaction Init Failed", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            throw new ApiException("Transaction Init Failed", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        // ---- provider call (no DB transaction held)
        Map<String, Object> providerResponse = spec.dispatch().call(init.txn(), service, user, sender);

        // ---- phase 2: response handling
        try {
            return tx.execute(status -> {
                ResponseEntity<Map<String, Object>> result =
                        spec.handler().handle(init.txn(), providerResponse, user, service, sender);
                auditLogService.updateResponse(init.audit().getId(), Json.write(providerResponse));
                return result;
            });
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Transaction Update Failed", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
