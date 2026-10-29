package com.securepay.modules.fundrequest.repository;

import com.securepay.modules.fundrequest.entity.FundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Repository for {@link FundRequest}. Extends {@link JpaSpecificationExecutor} for the dynamic
 * getFundRequests filters (status / user_id / date range) with pagination.
 */
public interface FundRequestRepository
        extends JpaRepository<FundRequest, Long>, JpaSpecificationExecutor<FundRequest> {

    /** Duplicate guard: a client_ref must be unique across fund requests. */
    Optional<FundRequest> findByClientRef(String clientRef);

    boolean existsByClientRef(String clientRef);
}
