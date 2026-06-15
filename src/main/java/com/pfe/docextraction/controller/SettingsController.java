package com.pfe.docextraction.controller;

import com.pfe.docextraction.entity.AppSettings;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.repository.AppSettingsRepository;
import com.pfe.docextraction.service.audit.AuditService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final AuditService auditService;
    private final AppSettingsRepository appSettingsRepository;

    public SettingsController(AuditService auditService, AppSettingsRepository appSettingsRepository) {
        this.auditService = auditService;
        this.appSettingsRepository = appSettingsRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings() {
        AppSettings settings = appSettingsRepository.findTopByOrderByCreatedAtDesc()
                .orElseGet(() -> appSettingsRepository.save(buildDefaultSettings()));
        return ResponseEntity.ok(toResponse(settings));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateSettings(
            @RequestBody Map<String, Object> settings,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        AppSettings current = appSettingsRepository.findTopByOrderByCreatedAtDesc()
                .orElseGet(AppSettings::new);

        applySettings(current, settings);
        AppSettings saved = appSettingsRepository.save(current);

        auditService.logAction(user, "SETTINGS_UPDATE", "SETTINGS", "global");
        return ResponseEntity.ok(toResponse(saved));
    }

    private AppSettings buildDefaultSettings() {
        AppSettings settings = new AppSettings();
        settings.setOrganisationNom("ExtractAI");
        settings.setOrganisationEmail("contact@extractai.com");
        settings.setOrganisationTelephone("+216 71 123 456");
        settings.setOrganisationPays("Tunisie");
        settings.setExtractionSeuilAuto(85);
        settings.setExtractionLangue("Français");
        settings.setExtractionScoreMin(50);
        settings.setNotificationsEmail(false);
        settings.setNotificationsAlerteErreur(false);
        settings.setNotificationsApprentissageContinu(false);
        settings.setNotificationsAmeliorerModele(false);
        settings.setSecuriteDeuxFacteurs(false);
        settings.setSecuriteDeuxFacteursType("TOTP");
        settings.setSecuriteDureeSession(8);
        return settings;
    }

    private void applySettings(AppSettings target, Map<String, Object> settings) {
        Map<String, Object> organisation = getNestedMap(settings, "organisation");
        Map<String, Object> extraction = getNestedMap(settings, "extraction");
        Map<String, Object> notifications = getNestedMap(settings, "notifications");
        Map<String, Object> securite = getNestedMap(settings, "securite");

        target.setOrganisationNom(getString(organisation, "nom", ""));
        target.setOrganisationEmail(getString(organisation, "email", ""));
        target.setOrganisationTelephone(getString(organisation, "telephone", ""));
        target.setOrganisationPays(getString(organisation, "pays", "Tunisie"));

        target.setExtractionSeuilAuto(getInteger(extraction, "seuilAuto", 85));
        target.setExtractionLangue(getString(extraction, "langue", "Français"));
        target.setExtractionScoreMin(getInteger(extraction, "scoreMin", 50));

        target.setNotificationsEmail(getBoolean(notifications, "email", false));
        target.setNotificationsAlerteErreur(getBoolean(notifications, "alerteErreur", false));
        target.setNotificationsApprentissageContinu(getBoolean(notifications, "apprentissageContinu", false));
        target.setNotificationsAmeliorerModele(getBoolean(notifications, "ameliorerModele", false));

        target.setSecuriteDeuxFacteurs(getBoolean(securite, "deuxFacteurs", false));
        target.setSecuriteDeuxFacteursType(getString(securite, "deuxFacteursType", "TOTP"));
        target.setSecuriteDureeSession(getInteger(securite, "dureeSession", 8));
    }

    private Map<String, Object> toResponse(AppSettings settings) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("organisation", Map.of(
                "nom", settings.getOrganisationNom(),
                "email", settings.getOrganisationEmail(),
                "telephone", settings.getOrganisationTelephone(),
                "pays", settings.getOrganisationPays()
        ));
        response.put("extraction", Map.of(
                "seuilAuto", settings.getExtractionSeuilAuto(),
                "langue", settings.getExtractionLangue(),
                "scoreMin", settings.getExtractionScoreMin()
        ));
        response.put("notifications", Map.of(
                "email", settings.getNotificationsEmail(),
                "alerteErreur", settings.getNotificationsAlerteErreur(),
                "apprentissageContinu", settings.getNotificationsApprentissageContinu(),
                "ameliorerModele", settings.getNotificationsAmeliorerModele()
        ));
        response.put("securite", Map.of(
                "deuxFacteurs", settings.getSecuriteDeuxFacteurs(),
                "deuxFacteursType", settings.getSecuriteDeuxFacteursType(),
                "dureeSession", settings.getSecuriteDureeSession()
        ));
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(Map<String, Object> settings, String key) {
        Object value = settings.get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new HashMap<>();
    }

    private String getString(Map<String, Object> source, String key, String defaultValue) {
        Object value = source.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Integer getInteger(Map<String, Object> source, String key, Integer defaultValue) {
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private Boolean getBoolean(Map<String, Object> source, String key, Boolean defaultValue) {
        Object value = source.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return defaultValue;
    }
}
