package com.securepay.modules.auditlog.repository;

import com.securepay.modules.auditlog.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for {@link AuditLog}. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(Long userId);
}
