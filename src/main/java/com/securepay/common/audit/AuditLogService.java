package com.securepay.common.audit;

import com.securepay.modules.auditlog.entity.AuditLog;
import com.securepay.modules.auditlog.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

/**
 * Writes audit log rows for logins and transactions, as the source does (doc 02 §2.1, doc 07 §7.1).
 */
@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public AuditLog record(Long userId, String role, String action, String serviceName,
                           String requestData, String responseData, String ipAddress,
                           String userAgent, String status) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setRole(role);
        log.setAction(action);
        log.setServiceName(serviceName);
        log.setRequestData(requestData);
        log.setResponseData(responseData);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setStatus(status);
        return repository.save(log);
    }

    /** Port of {@code AuditService::logRequest}: status INITIATED, request payload captured. */
    public AuditLog logRequest(Long userId, String role, String serviceName, String requestJson,
                               String ip, String clientRef, String userAgent) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setRole(role);
        log.setAction(serviceName);
        log.setServiceName(serviceName);
        log.setRequestData(requestJson);
        log.setIpAddress(ip);
        log.setClientRef(clientRef);
        log.setUserAgent(userAgent);
        log.setStatus("INITIATED");
        return repository.save(log);
    }

    /** Port of {@code AuditService::updateResponse}: stores the provider response, status SUCCESS. */
    public void updateResponse(Long auditLogId, String responseJson) {
        repository.findById(auditLogId).ifPresent(a -> {
            a.setResponseData(responseJson);
            a.setStatus("SUCCESS");
            repository.save(a);
        });
    }
}
