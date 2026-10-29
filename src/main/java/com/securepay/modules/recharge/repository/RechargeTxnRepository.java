package com.securepay.modules.recharge.repository;

import com.securepay.modules.recharge.entity.RechargeTxn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repository for {@link RechargeTxn}. */
public interface RechargeTxnRepository extends JpaRepository<RechargeTxn, Long> {
    List<RechargeTxn> findByUserId(Long userId);

    Optional<RechargeTxn> findByTxnId(String txnId);
}
