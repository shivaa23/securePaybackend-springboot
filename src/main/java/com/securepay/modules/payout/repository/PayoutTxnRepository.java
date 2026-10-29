package com.securepay.modules.payout.repository;

import com.securepay.modules.payout.entity.PayoutTxn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Repository for {@link PayoutTxn}. */
public interface PayoutTxnRepository extends JpaRepository<PayoutTxn, Long>, JpaSpecificationExecutor<PayoutTxn> {

    List<PayoutTxn> findByUserId(Long userId);

    Optional<PayoutTxn> findByTxnId(String txnId);

    /** Validation rule {@code unique:payout_txns} on client_ref. */
    boolean existsByClientRef(String clientRef);

    /** Duplicate-payout guard: same account + amount, PENDING/SUCCESS, created at/after {@code since}. */
    boolean existsByAccountNumberAndAmountAndStatusInAndCreatedAtGreaterThanEqual(
            String accountNumber, BigDecimal amount, Collection<String> statuses, LocalDateTime since);
}
