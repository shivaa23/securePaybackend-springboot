package com.securepay.modules.risk.repository;

import com.securepay.modules.risk.entity.BlackListedAcc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository for {@link BlackListedAcc}. */
public interface BlackListedAccRepository extends JpaRepository<BlackListedAcc, Long> {
    Optional<BlackListedAcc> findByAccNo(String accNo);
}
