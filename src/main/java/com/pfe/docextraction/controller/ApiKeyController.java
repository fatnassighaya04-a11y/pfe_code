package com.pfe.docextraction.controller;

import com.pfe.docextraction.entity.ApiKey;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.repository.ApiKeyRepository;
import com.pfe.docextraction.service.ApiKeyService;
import com.pfe.docextraction.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final ApiKeyRepository apiKeyRepository;
    private final AuditService auditService;

    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllKeys(@AuthenticationPrincipal User user) {
        List<ApiKey> keys = apiKeyRepository.findAll();
        List<Map<String, Object>> response = keys.stream()
                .map(key -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", key.getId());
                    map.put("appName", key.getAppName());
                    map.put("keyPreview", maskKey(key.getKeyValue())); // masquer la clé
                    map.put("isActive", key.getIsActive());
                    map.put("expiresAt", key.getExpiresAt());
                    map.put("createdAt", key.getCreatedAt());
                    map.put("lastUsedAt", key.getLastUsedAt());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> generateKey(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User user) {

        String appName = (String) body.getOrDefault("appName", "Application externe");
        int expiresInDays = ((Number) body.getOrDefault("expiresInDays", 90)).intValue();

        ApiKey newKey = apiKeyService.generateKey(appName, user, expiresInDays);
        auditService.logAction(user, "API_KEY_GENERATE", "API_KEY", newKey.getId().toString());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", newKey.getId());
        response.put("keyValue", newKey.getKeyValue()); // clé complète (à montrer une seule fois)
        response.put("appName", newKey.getAppName());
        response.put("expiresAt", newKey.getExpiresAt());
        response.put("message", "Clé générée avec succès");
        return ResponseEntity.ok(response);
    }

    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> revokeKey(@PathVariable UUID id) {
        apiKeyService.revokeKey(id);
        auditService.logAction(null, "API_KEY_REVOKE", "API_KEY", id.toString());
        return ResponseEntity.ok(Map.of("message", "Clé révoquée"));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> approveKey(@PathVariable UUID id, @AuthenticationPrincipal User admin) {
        apiKeyService.approveKey(id);
        auditService.logAction(admin, "API_KEY_APPROVE", "API_KEY", id.toString());
        return ResponseEntity.ok(Map.of("message", "Clé approuvée"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateKey(@PathVariable UUID id,
                                                         @RequestBody Map<String, Object> body,
                                                         @AuthenticationPrincipal User admin) {
        String appName = (String) body.get("appName");
        Integer expiresInDays = body.containsKey("expiresInDays") && body.get("expiresInDays") != null
            ? ((Number) body.get("expiresInDays")).intValue()
            : null;
        ApiKey updated = apiKeyService.updateKey(id, appName, expiresInDays);
        auditService.logAction(admin, "API_KEY_UPDATE", "API_KEY", id.toString());
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Clé mise à jour");
        resp.put("id", updated.getId());
        resp.put("appName", updated.getAppName());
        resp.put("expiresAt", updated.getExpiresAt());
        return ResponseEntity.ok(resp);
    }

    
    private String maskKey(String key) {
        if (key == null || key.length() < 10) return "***";
        return key.substring(0, 8) + "..." + key.substring(key.length() - 4);
    }
}