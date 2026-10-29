package com.securepay.modules.wallet.service;

import java.math.BigDecimal;

/**
 * Minimal view of a module transaction needed by the wallet ledger services (doc 10 §10.3).
 * Module txn entities (DMT, AEPS, Payout, ...) implement this so the shared wallet services can
 * read the charge/commission/TDS fields without coupling to a concrete type.
 *
 * <p>All monetary getters return RUPEES (as stored on the txn), matching the source where the
 * wallet services multiply by 100 to obtain paise.</p>
 */
public interface LedgerTxn {

    String getTxnId();

    default String getClientRef() {
        return null;
    }

    default String getOperator() {
        return null;
    }

    default BigDecimal getCharges() {
        return BigDecimal.ZERO;
    }

    default BigDecimal getCcf() {
        return BigDecimal.ZERO;
    }

    default BigDecimal getRetComm() {
        return BigDecimal.ZERO;
    }

    default BigDecimal getDiComm() {
        return BigDecimal.ZERO;
    }

    default BigDecimal getMdComm() {
        return BigDecimal.ZERO;
    }

    default BigDecimal getRetTds() {
        return BigDecimal.ZERO;
    }

    default BigDecimal getDiTds() {
        return BigDecimal.ZERO;
    }

    default BigDecimal getMdTds() {
        return BigDecimal.ZERO;
    }
}
