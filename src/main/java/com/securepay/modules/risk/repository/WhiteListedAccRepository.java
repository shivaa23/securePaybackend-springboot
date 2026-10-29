package com.securepay.modules.risk.repository;

import com.securepay.modules.risk.entity.WhiteListedAcc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository for {@link WhiteListedAcc}. */
public interface WhiteListedAccRepository extends JpaRepository<WhiteListedAcc, Long> {
    Optional<WhiteListedAcc> findByAccNo(String accNo);
}
