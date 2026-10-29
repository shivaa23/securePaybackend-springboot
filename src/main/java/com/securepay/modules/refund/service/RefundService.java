package com.securepay.modules.refund.service;

import com.securepay.common.exception.ApiException;
import com.securepay.common.money.Money;
import com.securepay.modules.aeps.entity.AepsTxn;
import com.securepay.modules.aeps.repository.AepsTxnRepository;
import com.securepay.modules.service.entity.ServiceEntity;
import com.securepay.modules.service.repository.ServiceRepository;
import com.securepay.modules.user.entity.User;
import com.securepay.modules.user.repository.UserRepository;
import com.securepay.modules.wallet.entity.WalletLedger;
import com.securepay.modules.wallet.repository.WalletLedger2Repository;
import com.securepay.modules.wallet.repository.WalletLedgerRepository;
import com.securepay.modules.wallet.service.WalletService2;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Port of the money-moving parts of {@code RefundModule/Controllers/RefundController}. */
@Service
public class RefundService {

    /** txn_id prefix → table for refundTxn (order matters, first match wins). */
    private static final Map<String, String> REFUND_TABLES = new LinkedHashMap<>();
    /** txn_id prefix → table for successTxn / failedTxn. */
    public static final Map<String, String> STATUS_TABLES = new LinkedHashMap<>();

    static {
        REFUND_TABLES.put("PMT", "payout_txns");
        REFUND_TABLES.put("DMT1", "dmt_txns");
        REFUND_TABLES.put("DMT2", "dmt_txns");
        REFUND_TABLES.put("ACV", "verification_txns");
        REFUND_TABLES.put("BBP", "bbps_txns");
        REFUND_TABLES.put("RCH", "recharge_txns");
        REFUND_TABLES.put("CARD", "credit_cards");
        STATUS_TABLES.put("PMT", "payout_txns");
        STATUS_TABLES.put("DMT1", "dmt_txns");
        STATUS_TABLES.put("DMT2", "dmt_txns");
        STATUS_TABLES.put("AEPS", "aeps_txns");
        STATUS_TABLES.put("ACV", "verification_txns");
        STATUS_TABLES.put("BBP", "bbps_txns");
        STATUS_TABLES.put("RCH", "recharge_txns");
        STATUS_TABLES.put("CARD", "credit_cards");
    }

    private final JdbcTemplate jdbc;
    private final WalletLedgerRepository ledger1;
    private final WalletLedger2Repository ledger2;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final AepsTxnRepository aepsTxnRepository;
    private final WalletService2 walletService2;

    public RefundService(JdbcTemplate jdbc, WalletLedgerRepository ledger1, WalletLedger2Repository ledger2,
                         UserRepository userRepository, ServiceRepository serviceRepository,
                         AepsTxnRepository aepsTxnRepository, WalletService2 walletService2) {
        this.jdbc = jdbc;
        this.ledger1 = ledger1;
        this.ledger2 = ledger2;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.aepsTxnRepository = aepsTxnRepository;
        this.walletService2 = walletService2;
    }

    private static ApiException err(String msg, HttpStatus st, Object errors) {
        return new ApiException(msg, st, errors);
    }

    private static BigDecimal num(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }

    private boolean tableExists(String table) {
        Integer n = jdbc.queryForObject("select count(*) from information_schema.tables "
                + "where table_schema = database() and table_name = ?", Integer.class, table);
        return n != null && n > 0;
    }

    /** Port of {@code refundTxn}: reverse every W1 ledger row of the txn and mark it REFUNDED. */
    @Transactional
    public Map<String, Object> refundTxn(String txnId) {
        List<WalletLedger> entries = ledger1.findByTxnId(txnId);
        if (entries.isEmpty()) {
            throw err("Ledger not found for this transaction", HttpStatus.NOT_FOUND, List.of());
        }
        String table = null;
        for (Map.Entry<String, String> e : REFUND_TABLES.entrySet()) {
            if (txnId.startsWith(e.getKey())) {
                table = e.getValue();
                break;
            }
        }
        Map<String, Object> txn = null;
        if (table != null && tableExists(table)) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "select * from " + table + " where txn_id = ? limit 1", txnId);
            if (!rows.isEmpty()) {
                txn = rows.get(0);
            }
        }
        if (txn == null) {
            throw err("Transaction not found", HttpStatus.NOT_FOUND, List.of());
        }
        String status = String.valueOf(txn.get("status"));
        if (!"PENDING".equals(status) && !"SUCCESS".equals(status)) {
            throw err("Refund not allowed. Transaction status is " + status, HttpStatus.FORBIDDEN, List.of());
        }
        List<Long> refundedUsers = new ArrayList<>();
        for (WalletLedger entry : entries) {
            refundedUsers.add(entry.getUserId());
            User user = userRepository.findByIdForUpdate(entry.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            long opening = user.getW1() == null ? 0L : user.getW1();
            long amount = Money.toPaise(entry.getAmount());
            boolean wasDebit = "DR".equals(entry.getTxnType());
            boolean wasCredit = "CR".equals(entry.getTxnType());
            if (wasDebit) {
                user.incrementW1(amount);
            } else if (wasCredit) {
                user.decrementW1(amount);
            }
            WalletLedger refund = new WalletLedger();
            refund.setType("refund");
            refund.setUserId(user.getId());
            refund.setService(entry.getService());
            refund.setTxnId(txnId);
            refund.setTxnType(wasDebit ? "CR" : wasCredit ? "DR" : null);
            refund.setAmount(Money.toRupees(amount));
            refund.setOpeningBalance(Money.toRupees(opening));
            refund.setClosingBalance(Money.toRupees(user.getW1() == null ? 0L : user.getW1()));
            refund.setNarration(wasDebit ? "Refund for " + entry.getService()
                    : wasCredit ? "Commission reversal for " + entry.getService() : null);
            refund.setStatus("SUCCESS");
            ledger1.save(refund);
            userRepository.save(user);
        }
        jdbc.update("update " + table + " set status = 'REFUNDED' where txn_id = ?", txnId);

        // Reverse the user's daily service summary when that table exists in this schema.
        if (tableExists("service_summaries") && txn.get("created_at") != null) {
            String day = txn.get("created_at").toString().substring(0, 10);
            jdbc.update("update service_summaries set a_comm = a_comm - ?, di_comm = di_comm - ?, "
                            + "total_amount = total_amount - ?, di_tds = di_tds - ?, md_comm = md_comm - ?, "
                            + "md_tds = md_tds - ?, total_charges = total_charges - ?, total_comm = total_comm - ?, "
                            + "total_tds = total_tds - ? where user_id = ? and summary_date = ?",
                    num(txn.get("a_comm")), num(txn.get("di_comm")), num(txn.get("amount")),
                    num(txn.get("di_tds")), num(txn.get("md_comm")), num(txn.get("md_tds")),
                    num(txn.get("charges")), num(txn.get("ret_comm")), num(txn.get("ret_tds")),
                    txn.get("user_id"), day);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("txn_id", txnId);
        out.put("refunded_users", refundedUsers);
        return out;
    }

    /** Port of {@code successTxn} / {@code failedTxn}: flip a pending txn's status and operator id. */
    @Transactional
    public Map<String, Object> markStatus(String txnId, String operatorId, String newStatus,
                                          List<String> allowedFrom) {
        for (Map.Entry<String, String> e : STATUS_TABLES.entrySet()) {
            if (!txnId.startsWith(e.getKey())) {
                continue;
            }
            String table = e.getValue();
            int updated = 0;
            if (tableExists(table)) {
                // the source writes AEPS' RRN column; in this schema that value lives in operator_id
                String in = String.join(",", allowedFrom.stream().map(s -> "'" + s + "'").toList());
                updated = jdbc.update("update " + table + " set operator_id = ?, status = ? "
                        + "where txn_id = ? and status in (" + in + ")", operatorId, newStatus, txnId);
            }
            if (updated == 0) {
                throw err("Transaction already altered or not found", HttpStatus.FORBIDDEN, "");
            }
            return Map.of();
        }
        throw err("Transaction Not Found", HttpStatus.NOT_FOUND, List.of());
    }

    /** Port of {@code successAepsTxn}: credit the W2 wallet + commissions of a pending AEPS withdrawal. */
    @Transactional
    public Map<String, Object> successAepsTxn(String txnId) {
        if (ledger2.existsByTxnId(txnId)) {
            throw err("Transaction already processed", HttpStatus.BAD_REQUEST, List.of());
        }
        if (!txnId.startsWith("AEPS")) {
            throw err("Invalid Transaction Prefix", HttpStatus.BAD_REQUEST, List.of());
        }
        AepsTxn txn = aepsTxnRepository.findByTxnId(txnId)
                .orElseThrow(() -> err("Transaction not found", HttpStatus.NOT_FOUND, List.of()));
        if (!"PENDING".equals(txn.getStatus())) {
            throw err("Refund not allowed. Current status: " + txn.getStatus(), HttpStatus.FORBIDDEN, List.of());
        }
        String op = txn.getOperator();
        if (!"Aeps 1 Cash Withdrawal".equals(op) && !"Aeps 2 Cash Withdrawal".equals(op)) {
            throw err("Invalid Operator/Service", HttpStatus.BAD_REQUEST, List.of());
        }
        ServiceEntity service = serviceRepository.findByName(op)
                .orElseThrow(() -> err("Invalid Operator/Service", HttpStatus.BAD_REQUEST, List.of()));
        User user = userRepository.findById(txn.getUserId())
                .orElseThrow(() -> err("User not found", HttpStatus.NOT_FOUND, List.of()));
        walletService2.credit(user, txn.getAmount(), txn, service);
        walletService2.creditCommission(user, txn, service);
        if (user.getParent() != null) {
            User distributor = userRepository.findById(user.getParent()).orElse(null);
            if (distributor != null && txn.getDiComm() != null && txn.getDiComm().signum() > 0) {
                walletService2.creditCommission(distributor, txn, service);
                userRepository.save(distributor);
            }
            if (distributor != null && distributor.getParent() != null) {
                User md = userRepository.findById(distributor.getParent()).orElse(null);
                if (md != null && txn.getMdComm() != null && txn.getMdComm().signum() > 0) {
                    walletService2.creditCommission(md, txn, service);
                    userRepository.save(md);
                }
            }
        }
        userRepository.save(user);
        txn.setStatus("SUCCESS");
        aepsTxnRepository.save(txn);
        return Map.of("txn_id", txnId);
    }
}
