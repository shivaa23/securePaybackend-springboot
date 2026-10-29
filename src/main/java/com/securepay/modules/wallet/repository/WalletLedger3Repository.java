package com.securepay.modules.wallet.repository;

import com.securepay.modules.wallet.entity.WalletLedger3;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for the W3 {@link WalletLedger3}. */
public interface WalletLedger3Repository extends JpaRepository<WalletLedger3, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<WalletLedger3> {
    List<WalletLedger3> findByUserId(Long userId);
}
