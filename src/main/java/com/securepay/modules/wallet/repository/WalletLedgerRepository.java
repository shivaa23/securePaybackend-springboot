package com.securepay.modules.wallet.repository;

import com.securepay.modules.wallet.entity.WalletLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for the W1 {@link WalletLedger}. */
public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<WalletLedger> {
    List<WalletLedger> findByUserId(Long userId);
    List<WalletLedger> findByTxnId(String txnId);
}
