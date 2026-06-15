package com.pfe.docextraction.controller;

import com.pfe.docextraction.entity.AuditLog;
import com.pfe.docextraction.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs;
        if (action != null && !action.isBlank()) {
            logs = auditLogRepository.findByActionAndCreatedAtBetweenOrderByCreatedAtDesc(
                action,
                java.time.LocalDateTime.now().minusYears(1),
                java.time.LocalDateTime.now(),
                pageable
            );
        } else {
            logs = auditLogRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(
                    java.time.LocalDateTime.now().minusYears(1),
                    java.time.LocalDateTime.now(),
                    pageable
                );
        }

        List<Map<String, Object>> content = logs.getContent().stream()
            .map(log -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", log.getId());
                map.put("action", log.getAction());
                map.put("resourceType", log.getResourceType());
                map.put("resourceId", log.getResourceId());
                map.put("result", log.getResult());
                map.put("ipAddress", log.getIpAddress());
                map.put("details", log.getDetails());
                map.put("createdAt", log.getCreatedAt());
                if (log.getUser() != null) {
                    map.put("userEmail", log.getUser().getEmail());
                }
                return map;
            })
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalElements", logs.getTotalElements());
        response.put("totalPages", logs.getTotalPages());
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getUserLogs(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditLogRepository
            .findByUserIdOrderByCreatedAtDesc(
                UUID.fromString(userId), pageable
            );

        List<Map<String, Object>> response = logs.getContent().stream()
            .map(log -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", log.getId());
                map.put("action", log.getAction());
                map.put("resourceType", log.getResourceType());
                map.put("result", log.getResult());
                map.put("createdAt", log.getCreatedAt());
                return map;
            })
            .toList();

        return ResponseEntity.ok(response);
    }
}