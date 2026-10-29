package com.securepay.modules.wallet.service;

import com.securepay.common.money.Money;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.wallet.entity.WalletLedger2;
import com.securepay.modules.wallet.repository.WalletLedger2Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * W2 wallet ledger — port of {@code WalletService2.php} (doc 10 §10.3).
 *
 * <p>Structurally identical to {@link WalletService} but operates on {@code w2}. The source does
 * not pessimistically lock in W2/W3; we mutate the passed user and persist the ledger, mirroring it.</p>
 */
@Service
public class WalletService2 {

    private final WalletLedger2Repository ledgerRepository;

    public WalletService2(WalletLedger2Repository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public WalletLedger2 credit(User user, BigDecimal amount, LedgerTxn txn, ServiceEntity service) {
        BigDecimal opening = Money.toRupees(nz(user.getW2()));
        long credit = Money.toPaise(Money.nz(amount)); // both branches identical in source
        user.incrementW2(credit);

        WalletLedger2 ledger = newLedger(service, user, txn.getTxnId(), "CR", credit, opening,
                "Wallet creditted for " + service.getName());
        return ledgerRepository.save(ledger);
    }

    @Transactional
    public WalletLedger2 debit(User user, BigDecimal amount, LedgerTxn txn, ServiceEntity service) {
        BigDecimal opening = Money.toRupees(nz(user.getW2()));
        long debit = Money.toPaise(Money.nz(amount));
        user.decrementW2(debit);

        WalletLedger2 ledger = newLedger(service, user, txn.getTxnId(), "DR", debit, opening,
                "Wallet creditted for " + service.getName());
        return ledgerRepository.save(ledger);
    }

    /** Commission credit for services {50,51,53,54}, net of TDS by role; else ret_comm (doc 10 §10.3). */
    @Transactional
    public WalletLedger2 creditCommission(User user, LedgerTxn txn, ServiceEntity service) {
        BigDecimal opening = Money.toRupees(nz(user.getW2()));
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
        user.incrementW2(credit);

        WalletLedger2 ledger = newLedger(service, user, txn.getTxnId(), "CR", credit, opening,
                "Wallet credited for Commission " + service.getName());
        return ledgerRepository.save(ledger);
    }

    private WalletLedger2 newLedger(ServiceEntity service, User user, String txnId, String txnType,
                                    long amountPaise, BigDecimal opening, String narration) {
        WalletLedger2 ledger = new WalletLedger2();
        ledger.setType(service.getName());
        ledger.setUserId(user.getId());
        ledger.setService(service.getName());
        ledger.setTxnId(txnId);
        ledger.setTxnType(txnType);
        ledger.setAmount(Money.toRupees(amountPaise));
        ledger.setOpeningBalance(opening);
        ledger.setClosingBalance(Money.toRupees(nz(user.getW2())));
        ledger.setNarration(narration);
        ledger.setStatus("SUCCESS");
        return ledger;
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }
}
