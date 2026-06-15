package com.pfe.docextraction.service.audit;

import com.pfe.docextraction.entity.AuditLog;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    // Enregistrer une action
    @Async
    public void log(User user, String action, String resourceType,
                    String resourceId, String result, String ipAddress,
                    String details) {

        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setResult(result);
        log.setIpAddress(ipAddress);
        log.setDetails(details);

        auditLogRepository.save(log);
    }

    // Méthode simplifiée pour les actions courantes
    @Async
    public void logAction(User user, String action,
                          String resourceType, String resourceId) {
        log(user, action, resourceType, resourceId, "SUCCESS", null, null);
    }
}