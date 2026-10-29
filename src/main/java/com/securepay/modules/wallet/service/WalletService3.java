package com.securepay.modules.wallet.service;

import com.securepay.common.money.Money;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.wallet.entity.WalletLedger3;
import com.securepay.modules.wallet.repository.WalletLedger3Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * W3 wallet ledger — port of {@code WalletService3.php} (doc 10 §10.3).
 *
 * <p>⚠️ Mirrors known source quirks verbatim (doc 10 §10.3): several {@code closing_balance} lines
 * read {@code w2} even in the W3 service, and {@code creditCommission} calls {@code incrementW2}.
 * These are preserved for parity and flagged below — confirm with product before "fixing".</p>
 */
@Service
public class WalletService3 {

    private final WalletLedger3Repository ledgerRepository;

    public WalletService3(WalletLedger3Repository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public WalletLedger3 credit(User user, BigDecimal amount, LedgerTxn txn, ServiceEntity service) {
        BigDecimal opening = Money.toRupees(nz(user.getW3()));
        long credit = Money.toPaise(Money.nz(amount));
        user.incrementW3(credit);

        WalletLedger3 ledger = new WalletLedger3();
        ledger.setType(service.getName());
        ledger.setUserId(user.getId());
        ledger.setService(service.getName());
        ledger.setTxnId(txn.getTxnId());
        ledger.setTxnType("CR");
        ledger.setAmount(Money.toRupees(credit));
        ledger.setOpeningBalance(opening);
        // NOTE: mirrors source quirk — closing_balance reads w2 even in the W3 service.
        ledger.setClosingBalance(Money.toRupees(nz(user.getW2())));
        ledger.setNarration("Wallet creditted for " + service.getName());
        ledger.setStatus("SUCCESS");
        return ledgerRepository.save(ledger);
    }

    @Transactional
    public WalletLedger3 debit(User user, BigDecimal amount, LedgerTxn txn, ServiceEntity service) {
        BigDecimal opening = Money.toRupees(nz(user.getW3()));
        long debit = Money.toPaise(Money.nz(amount));
        user.decrementW3(debit);

        WalletLedger3 ledger = new WalletLedger3();
        ledger.setType(service.getName());
        ledger.setUserId(user.getId());
        ledger.setService(service.getName());
        ledger.setTxnId(txn.getTxnId());
        ledger.setTxnType("DR");
        ledger.setAmount(Money.toRupees(debit));
        ledger.setOpeningBalance(opening);
        // NOTE: mirrors source quirk — closing_balance reads w2 even in the W3 service.
        ledger.setClosingBalance(Money.toRupees(nz(user.getW2())));
        ledger.setNarration("Wallet creditted for " + service.getName());
        ledger.setStatus("SUCCESS");
        return ledgerRepository.save(ledger);
    }

    @Transactional
    public WalletLedger3 creditCommission(User user, LedgerTxn txn, ServiceEntity service) {
        BigDecimal opening = Money.toRupees(nz(user.getW3()));
        long credit;
        Long sid = service.getId();
        boolean special = sid != null && (sid == 50 || sid == 51 || sid == 53 || sid == 54);
        if (special) {
            String role = user.getRole() == null ? "" : user.getRole();
            BigDecimal net = switch (role) {
                case "ret", "dd", "api" -> Money.nz(txn.getRetComm()).subtract(Money.nz(txn.getRetTds()));
                case "di" -> Money.nz(txn.getDiComm()).subtract(Money.nz(txn.getDiTds()));
                case "md" -> Money.nz(txn.getMdComm()).subtract(Money.nz(txn.getMdTds()));
                default -> Money.nz(txn.getRetComm());
            };
            credit = Money.toPaise(net);
        } else {
            credit = Money.toPaise(Money.nz(txn.getRetComm()));
        }
        // NOTE: mirrors source quirk — W3 creditCommission increments w2, not w3.
        user.incrementW2(credit);

        WalletLedger3 ledger = new WalletLedger3();
        ledger.setType(service.getName());
        ledger.setUserId(user.getId());
        ledger.setService(service.getName());
        ledger.setTxnId(txn.getTxnId());
        ledger.setTxnType("CR");
        ledger.setAmount(Money.toRupees(credit));
        ledger.setOpeningBalance(opening);
        // NOTE: mirrors source quirk — closing_balance reads w2.
        ledger.setClosingBalance(Money.toRupees(nz(user.getW2())));
        ledger.setNarration("Wallet credited for Commission " + service.getName());
        ledger.setStatus("SUCCESS");
        return ledgerRepository.save(ledger);
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }
}
