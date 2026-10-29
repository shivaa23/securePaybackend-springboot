package com.securepay.modules.aeps.repository;

import com.securepay.modules.aeps.entity.AepsTxn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repository for {@link AepsTxn}. */
public interface AepsTxnRepository extends JpaRepository<AepsTxn, Long> {
    List<AepsTxn> findByUserId(Long userId);

    Optional<AepsTxn> findByTxnId(String txnId);
}
