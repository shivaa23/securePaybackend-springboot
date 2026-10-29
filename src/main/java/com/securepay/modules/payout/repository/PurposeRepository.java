package com.securepay.modules.payout.repository;

import com.securepay.modules.payout.entity.Purpose;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link Purpose}. */
public interface PurposeRepository extends JpaRepository<Purpose, Long> {
}
