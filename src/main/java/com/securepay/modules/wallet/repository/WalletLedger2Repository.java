package com.securepay.modules.wallet.repository;

import com.securepay.modules.wallet.entity.WalletLedger2;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for the W2 {@link WalletLedger2}. */
public interface WalletLedger2Repository extends JpaRepository<WalletLedger2, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<WalletLedger2> {
    List<WalletLedger2> findByUserId(Long userId);
    boolean existsByTxnId(String txnId);
}
