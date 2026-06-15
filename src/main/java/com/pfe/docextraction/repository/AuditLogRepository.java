package com.pfe.docextraction.repository;

import com.pfe.docextraction.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<AuditLog> findByActionAndCreatedAtBetweenOrderByCreatedAtDesc(
        String action, LocalDateTime from, LocalDateTime to, Pageable pageable
    );

    List<AuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
        String resourceType, String resourceId
    );

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime from, LocalDateTime to, Pageable pageable
    );
}