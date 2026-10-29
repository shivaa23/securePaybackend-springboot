package com.securepay.modules.bank.repository;

import com.securepay.modules.bank.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for {@link Bank}. */
public interface BankRepository extends JpaRepository<Bank, Long> {

    List<Bank> findByStatus(Integer status);
}
