package com.securepay.modules.commission.repository;

import com.securepay.modules.commission.entity.AcommRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

/** Repository for {@link AcommRule}. Encodes the amount-band predicate (doc 10 §10.5). */
public interface AcommRuleRepository extends JpaRepository<AcommRule, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<AcommRule> {

    @Query("""
            select r from AcommRule r
            where r.route = :route
              and r.serviceName = :serviceName
              and r.minAmount <= :amount
              and (r.maxAmount >= :amount or r.maxAmount = 0)
            order by r.id asc
            """)
    Optional<AcommRule> findMatching(@Param("route") String route,
                                     @Param("serviceName") String serviceName,
                                     @Param("amount") BigDecimal amount);
}
