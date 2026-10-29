package com.securepay.modules.payout.repository;

import com.securepay.modules.payout.entity.PayoutSender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** Repository for {@link PayoutSender}. */
public interface PayoutSenderRepository
        extends JpaRepository<PayoutSender, Long>, JpaSpecificationExecutor<PayoutSender> {

    Optional<PayoutSender> findFirstByMobileNumber(String mobileNumber);

    Optional<PayoutSender> findFirstByMobileNumberAndIsActive(String mobileNumber, Integer isActive);

    Optional<PayoutSender> findFirstByMobileNumberAndPipe(String mobileNumber, String pipe);

    Optional<PayoutSender> findFirstByMobileNumberAndPipeAndIsActive(String mobileNumber, String pipe, Integer isActive);

    boolean existsByMobileNumber(String mobileNumber);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update PayoutSender s set s.remLimit = :limit")
    int setAllRemLimit(@org.springframework.data.repository.query.Param("limit") java.math.BigDecimal limit);
}
