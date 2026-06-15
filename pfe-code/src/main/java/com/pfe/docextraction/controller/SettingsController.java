package com.pfe.docextraction.controller;

import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.service.audit.AuditService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@lombok.RequiredArgsConstructor
public class SettingsController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("organisation", Map.of(
            "nom", "ExtractAI",
            "email", "contact@extractai.com",
            "telephone", "+216 71 123 456",
            "pays", "Tunisie"
        ));
        settings.put("extraction", Map.of(
            "seuilAuto", 85,
            "langue", "Français",
            "scoreMin", 50
        ));
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateSettings(
            @RequestBody Map<String, Object> settings,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        System.out.println("Settings reçus : " + settings);
        auditService.logAction(user, "SETTINGS_UPDATE", "SETTINGS", "global");
        return ResponseEntity.ok(settings);
    }
}
