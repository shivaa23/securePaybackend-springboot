package com.securepay.modules.kyc.repository;

import com.securepay.modules.kyc.entity.Kyc;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link Kyc}. */
public interface KycRepository extends JpaRepository<Kyc, Long> {
}
