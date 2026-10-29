package com.securepay.modules.account.repository;

import com.securepay.modules.account.entity.AccountStatement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository for {@link AccountStatement}. */
public interface AccountStatementRepository extends JpaRepository<AccountStatement, Long> {

    Page<AccountStatement> findByAccountIdOrderByIdDesc(Long accountId, Pageable pageable);

    Optional<AccountStatement> findFirstByAccountIdOrderByIdDesc(Long accountId);
}
