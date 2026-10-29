package com.securepay.modules.wallet.service;

import com.securepay.common.exception.InsufficientBalanceException;
import com.securepay.common.exception.WalletIntegrityException;
import com.securepay.common.money.Money;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import com.securepay.modules.wallet.entity.WalletLedger;
import com.securepay.modules.wallet.repository.WalletLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * W1 wallet ledger — port of {@code WalletService.php} (doc 10 §10.3).
 *
 * <p>Each mutation runs in a single {@code @Transactional} method with a pessimistic lock on the
 * user row ({@code findByIdForUpdate}), computing opening/closing balances atomically. Wallet math
 * is in paise (long); ledger rows store rupees.</p>
 */
@Service
public class WalletService {

    /** Service ids whose commission credit is net of TDS by role (doc 10 §10.3). */
    private static final Set<String> TDS_AWARE_SERVICE_IDS = Set.of("11", "13", "14", "24", "81", "336", "337");

    private final UserRepository userRepository;
    private final WalletLedgerRepository ledgerRepository;

    public WalletService(UserRepository userRepository, WalletLedgerRepository ledgerRepository) {
        this.userRepository = userRepository;
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Charge basis in paise based on {@code service->type} (doc 10 §10.3):
     * AMTCHG → amount+charges, CHG → charges, AMTCCF → amount+ccf, else → amount.
     */
    private long basisPaise(BigDecimal amount, LedgerTxn txn, ServiceEntity service) {
        String type = service.getType();
        BigDecimal rupees;
        if ("AMTCHG".equals(type)) {
            rupees = Money.nz(amount).add(Money.nz(txn.getCharges()));
        } else if ("CHG".equals(type)) {
            rupees = Money.nz(txn.getCharges());
        } else if ("AMTCCF".equals(type)) {
            rupees = Money.nz(amount).add(Money.nz(txn.getCcf()));
        } else {
            rupees = Money.nz(amount);
        }
        return Money.toPaise(rupees);
    }

    /** Port of {@code WalletService::debit}. */
    @Transactional
    public WalletLedger debit(User user, BigDecimal amount, LedgerTxn txn, ServiceEntity service) {
        long debit = basisPaise(amount, txn, service);
        User locked = lock(user.getId());
        long openingPaise = nz(locked.getW1());

        // Pre-check: insufficient balance.
        if (nz(locked.getW1()) < debit) {
            throw new InsufficientBalanceException();
        }

        locked.decrementW1(debit);

        // Post-check: negative integrity guard (decrementW1 allows negative).
        if (nz(locked.getW1()) < 0) {
            throw new WalletIntegrityException();
        }

        WalletLedger ledger = new WalletLedger();
        ledger.setType(service.getName());
        ledger.setUserId(locked.getId());
        ledger.setService(service.getName());
        ledger.setTxnId(txn.getTxnId());
        ledger.setTxnType("DR");
        ledger.setAmount(Money.toRupees(debit));
        ledger.setOpeningBalance(Money.toRupees(openingPaise));
        ledger.setClosingBalance(Money.toRupees(nz(locked.getW1())));
        ledger.setNarration("Wallet debited for " + service.getName());
        ledger.setStatus("SUCCESS");
        return ledgerRepository.save(ledger);
    }

    /** Port of {@code WalletService::credit}. */
    @Transactional
    public WalletLedger credit(User user, BigDecimal amount, LedgerTxn txn, ServiceEntity service) {
        long credit = basisPaise(amount, txn, service);
        User locked = lock(user.getId());
        long openingPaise = nz(locked.getW1());

        locked.incrementW1(credit);

        WalletLedger ledger = new WalletLedger();
        ledger.setType(service.getName());
        ledger.setUserId(locked.getId());
        ledger.setService(service.getName());
        ledger.setTxnId(txn.getTxnId());
        ledger.setTxnType("CR");
        ledger.setAmount(Money.toRupees(credit));
        ledger.setOpeningBalance(Money.toRupees(openingPaise));
        ledger.setClosingBalance(Money.toRupees(nz(locked.getW1())));
        ledger.setNarration("Wallet credited for " + service.getName());
        ledger.setStatus("SUCCESS");
        return ledgerRepository.save(ledger);
    }

    /** Port of {@code WalletService::creditFundRequest}. */
    @Transactional
    public WalletLedger creditFundRequest(User user, BigDecimal amount, LedgerTxn txn) {
        User locked = lock(user.getId());
        long openingPaise = nz(locked.getW1());
        long credit = Money.toPaise(Money.nz(amount));

        locked.incrementW1(credit);

        WalletLedger ledger = new WalletLedger();
        ledger.setType("Fund Request");
        ledger.setUserId(locked.getId());
        ledger.setService("Fund Request");
        ledger.setTxnId(txn.getClientRef());
        ledger.setTxnType("CR");
        ledger.setAmount(Money.toRupees(credit));
        ledger.setOpeningBalance(Money.toRupees(openingPaise));
        ledger.setClosingBalance(Money.toRupees(nz(locked.getW1())));
        ledger.setNarration("Wallet credited for Fund Request");
        ledger.setStatus("SUCCESS");
        return ledgerRepository.save(ledger);
    }

    /** Port of {@code WalletService::creditCommission} — TDS-aware for specific service ids. */
    @Transactional
    public WalletLedger creditCommission(User user, BigDecimal amount, LedgerTxn txn, ServiceEntity service) {
        User locked = lock(user.getId());
        long openingPaise = nz(locked.getW1());

        long credit;
        String serviceId = service.getId() == null ? null : service.getId().toString();
        if (serviceId != null && TDS_AWARE_SERVICE_IDS.contains(serviceId)) {
            String role = locked.getRole();
            BigDecimal net;
            switch (role == null ? "" : role) {
                case "ret", "dd", "api" -> net = Money.nz(amount).subtract(Money.nz(txn.getRetTds()));
                case "di" -> net = Money.nz(amount).subtract(Money.nz(txn.getDiTds()));
                case "md" -> net = Money.nz(amount).subtract(Money.nz(txn.getMdTds()));
                default -> net = Money.nz(amount);
            }
            credit = Money.toPaise(net);
        } else {
            credit = Money.toPaise(Money.nz(amount));
        }

        locked.incrementW1(credit);

        WalletLedger ledger = new WalletLedger();
        ledger.setType("COMMISSION");
        ledger.setUserId(locked.getId());
        ledger.setService(service.getName() != null ? service.getName() : "Unknown");
        ledger.setTxnId(txn.getTxnId() != null ? txn.getTxnId() : uniqid("CM_"));
        ledger.setTxnType("CR");
        ledger.setAmount(Money.toRupees(credit));
        ledger.setOpeningBalance(Money.toRupees(openingPaise));
        ledger.setClosingBalance(Money.toRupees(nz(locked.getW1())));
        ledger.setNarration("Wallet credited for "
                + (service.getName() != null ? service.getName() : "Service") + " commission");
        ledger.setStatus("SUCCESS");
        return ledgerRepository.save(ledger);
    }

    /** Port of {@code WalletService::refund}. */
    @Transactional
    public WalletLedger refund(User user, BigDecimal amount, LedgerTxn txn, ServiceEntity service) {
        User locked = lock(user.getId());
        long openingPaise = nz(locked.getW1());
        long credit = basisPaise(amount, txn, service);

        locked.incrementW1(credit);

        WalletLedger ledger = new WalletLedger();
        ledger.setType("REFUND");
        ledger.setUserId(locked.getId());
        ledger.setService(txn.getOperator() != null ? txn.getOperator()
                : (service.getName() != null ? service.getName() : "Unknown"));
        ledger.setTxnId(txn.getTxnId() != null ? txn.getTxnId() : uniqid("RF_"));
        ledger.setTxnType("CR");
        ledger.setAmount(Money.toRupees(credit));
        ledger.setOpeningBalance(Money.toRupees(openingPaise));
        ledger.setClosingBalance(Money.toRupees(nz(locked.getW1())));
        ledger.setNarration("Wallet credited for refund of failed "
                + (service.getName() != null ? service.getName() : "transaction"));
        ledger.setStatus("SUCCESS");
        return ledgerRepository.save(ledger);
    }

    public List<WalletLedger> ledgersForUser(Long userId) {
        return ledgerRepository.findByUserId(userId);
    }

    private User lock(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for wallet lock: " + userId));
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    /** Equivalent of PHP {@code uniqid($prefix)} — prefix + time-based unique suffix (doc 10 §10.3). */
    private static String uniqid(String prefix) {
        long now = System.nanoTime();
        return prefix + Long.toHexString(now);
    }
}
