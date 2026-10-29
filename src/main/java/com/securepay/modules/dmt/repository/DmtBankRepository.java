package com.securepay.modules.dmt.repository;

import com.securepay.modules.dmt.entity.DmtBank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for {@link DmtBank}. */
public interface DmtBankRepository extends JpaRepository<DmtBank, Long> {
    List<DmtBank> findByPipeOrderByNameAsc(String pipe);
}
