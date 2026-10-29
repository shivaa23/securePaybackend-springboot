package com.securepay.modules.dmt.repository;

import com.securepay.modules.dmt.entity.DmtTxn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repository for {@link DmtTxn}. */
public interface DmtTxnRepository extends JpaRepository<DmtTxn, Long> {
    List<DmtTxn> findByUserId(Long userId);

    Optional<DmtTxn> findByTxnId(String txnId);
}
