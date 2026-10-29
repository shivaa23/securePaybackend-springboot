package com.securepay.modules.commission.repository;

import com.securepay.modules.commission.entity.CommRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

/** Repository for {@link CommRule}. Encodes the amount-band predicate (doc 10 §10.5). */
public interface CommRuleRepository extends JpaRepository<CommRule, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<CommRule> {

    /**
     * {@code min_amount <= amount AND (max_amount >= amount OR max_amount = 0)}, first match.
     */
    @Query("""
            select r from CommRule r
            where r.planId = :planId
              and r.serviceName = :serviceName
              and r.minAmount <= :amount
              and (r.maxAmount >= :amount or r.maxAmount = 0)
            order by r.id asc
            """)
    Optional<CommRule> findMatching(@Param("planId") Long planId,
                                    @Param("serviceName") String serviceName,
                                    @Param("amount") BigDecimal amount);
}
