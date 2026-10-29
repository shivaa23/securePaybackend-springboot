package com.securepay.modules.fundrequest.repository;

import com.securepay.modules.fundrequest.entity.FundTxnId;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link FundTxnId}. */
public interface FundTxnIdRepository extends JpaRepository<FundTxnId, Long> {
}
