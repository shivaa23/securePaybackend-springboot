package com.securepay.modules.dmt.repository;

import com.securepay.modules.dmt.entity.VerificationTxn;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link VerificationTxn}. */
public interface VerificationTxnRepository extends JpaRepository<VerificationTxn, Long> {
    boolean existsByClientRef(String clientRef);
}
