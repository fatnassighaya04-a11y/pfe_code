package com.pfe.docextraction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.docextraction.entity.Extraction;
import com.pfe.docextraction.entity.ExtractionCorrection;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.enums.DocumentStatus;
import com.pfe.docextraction.enums.DocumentType;
import com.pfe.docextraction.enums.ExtractionStatus;
import com.pfe.docextraction.repository.DocumentRepository;
import com.pfe.docextraction.repository.ExtractionCorrectionRepository;
import com.pfe.docextraction.repository.ExtractionRepository;
import com.pfe.docextraction.service.audit.AuditService;
import com.pfe.docextraction.service.extraction.ExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/extractions")
@RequiredArgsConstructor
public class ExtractionController {

    private final ExtractionRepository extractionRepository;
    private final DocumentRepository documentRepository;
    private final ExtractionService extractionService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final ExtractionCorrectionRepository correctionRepository;

    @GetMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'OPERATEUR', 'LECTEUR')")
    public ResponseEntity<Map<String, Object>> getExtraction(@PathVariable String documentId) {
        UUID docUuid = UUID.fromString(documentId);
        List<Extraction> extractions = extractionRepository.findByDocumentIdOrderByCreatedAtDesc(docUuid);

        if (extractions.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "id", null,
                "status", "PENDING",
                "extractedData", "{}",
                "confidenceScore", 0,
                "createdAt", null,
                "isValidated", false,
                "autoValidated", false,
                "thresholdUsed", 85.0
            ));
        }

        Extraction extraction = extractions.get(0);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", extraction.getId());
        response.put("status", extraction.getStatus());
        response.put("extractedData", extraction.getExtractedData());
        response.put("confidenceScore", extraction.getConfidenceScore() != null ? extraction.getConfidenceScore().doubleValue() : 0);
        response.put("createdAt", extraction.getCreatedAt());
        response.put("isValidated", extraction.getValidatedAt() != null);
        response.put("validatedAt", extraction.getValidatedAt());
        response.put("validationNotes", extraction.getValidationNotes());

        boolean autoValidated = extraction.getValidatedAt() != null
                && extraction.getValidationNotes() != null
                && extraction.getValidationNotes().contains("Approuvé automatiquement");
        response.put("autoValidated", autoValidated);

        Double thresholdUsed = extraction.getAutoValidationThreshold();
        response.put("thresholdUsed", thresholdUsed != null ? thresholdUsed : 85.0);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'OPERATEUR')")
    public ResponseEntity<Map<String, Object>> validateExtraction(
            @PathVariable String id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {

        Extraction extraction = extractionRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Extraction non trouvée"));

        String action = (String) body.getOrDefault("action", "APPROVE");
        String notes = (String) body.getOrDefault("notes", "");
        String rejectionReason = (String) body.getOrDefault("rejectionReason", "");

        extraction.setValidatedBy(user);
        extraction.setValidatedAt(LocalDateTime.now());

        auditService.log(
            user,
            "EXTRACTION_VALIDATE",
            "EXTRACTION",
            extraction.getId().toString(),
            "SUCCESS",
            null,
            "Validation manuelle demandée par " + user.getEmail()
        );

        if ("REJECT".equals(action)) {
            String fullNote = "Rejeté par " + user.getEmail();
            if (!rejectionReason.isBlank()) fullNote += " — Motif: " + rejectionReason;
            if (!notes.isBlank()) fullNote += " — Commentaire: " + notes;
            extraction.setValidationNotes(fullNote);
            extraction.setStatus(ExtractionStatus.FAILED);
            var doc = extraction.getDocument();
            if (doc != null) {
                doc.setStatus(DocumentStatus.REJECTED);
                documentRepository.save(doc);
            }
            auditService.log(
                    user,
                    "EXTRACTION_REJECT",
                    "EXTRACTION",
                    extraction.getId().toString(),
                    "SUCCESS",
                    null,
                    fullNote
            );
        } else {
            String fullNote = "Validé manuellement par " + user.getEmail();
            if (!notes.isBlank()) fullNote += " — " + notes;
            extraction.setValidationNotes(fullNote);
            var doc = extraction.getDocument();
            if (doc != null) {
                doc.setStatus(DocumentStatus.VALIDATED);
                documentRepository.save(doc);
            }
            auditService.log(
                    user,
                    "EXTRACTION_APPROVE",
                    "EXTRACTION",
                    extraction.getId().toString(),
                    "SUCCESS",
                    null,
                    fullNote
            );
        }

        extractionRepository.save(extraction);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", extraction.getId());
        response.put("action", action);
        response.put("validatedAt", extraction.getValidatedAt());
        response.put("validatedBy", user.getEmail());
        response.put("message", "REJECT".equals(action) ? "Document rejeté" : "Document validé");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/update-fields")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'OPERATEUR')")
    public ResponseEntity<Map<String, Object>> updateExtractedFields(
            @PathVariable String id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {

        Extraction extraction = extractionRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Extraction non trouvée"));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> correctedFields = (Map<String, Object>) body.get("fields");
            if (correctedFields != null) {
                // Comparer avant/après pour tracer chaque correction (analytics)
                Map<String, Object> aiData = parseAiData(extraction.getExtractedData());
                DocumentType docType = extraction.getDocument() != null
                        ? extraction.getDocument().getDocumentType()
                        : DocumentType.AUTRE;
                trackCorrections(extraction, docType, aiData, correctedFields, user);

                String updatedJson = objectMapper.writeValueAsString(correctedFields);
                extraction.setExtractedData(updatedJson);
            }

            String note = "Champs corrigés manuellement par " + user.getEmail()
                    + " le " + LocalDateTime.now();
            extraction.setValidationNotes(note);
            extractionRepository.save(extraction);

                auditService.log(
                    user,
                    "EXTRACTION_UPDATE_FIELDS",
                    "EXTRACTION",
                    extraction.getId().toString(),
                    "SUCCESS",
                    null,
                    note
                );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Format invalide: " + e.getMessage()));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", extraction.getId());
        response.put("message", "Champs mis à jour");
        response.put("updatedBy", user.getEmail());

        return ResponseEntity.ok(response);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAiData(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * Compare la version IA et la version corrigée champ par champ.
     * Pour chaque champ modifié, sauvegarde une ExtractionCorrection.
     */
    private void trackCorrections(Extraction extraction,
                                  DocumentType documentType,
                                  Map<String, Object> aiData,
                                  Map<String, Object> humanData,
                                  User user) {
        if (humanData == null) return;
        for (Map.Entry<String, Object> entry : humanData.entrySet()) {
            String fieldName = entry.getKey();
            String humanValue = stringify(entry.getValue());
            String aiValue = stringify(aiData.get(fieldName));

            // On enregistre seulement si la valeur a vraiment changé
            if (!java.util.Objects.equals(humanValue, aiValue)) {
                ExtractionCorrection correction = ExtractionCorrection.builder()
                        .extraction(extraction)
                        .documentType(documentType)
                        .fieldName(fieldName)
                        .aiValue(aiValue)
                        .humanValue(humanValue)
                        .correctedBy(user)
                        .build();
                correctionRepository.save(correction);
            }
        }
    }

    private String stringify(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'OPERATEUR', 'LECTEUR')")
    public ResponseEntity<List<Map<String, Object>>> getAllExtractions() {
        List<Extraction> extractions = extractionRepository.findAll();
        List<Map<String, Object>> response = extractions.stream()
                .map(e -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", e.getId());
                    map.put("documentId", e.getDocument().getId());
                    map.put("status", e.getStatus());
                    map.put("confidenceScore", e.getConfidenceScore() != null ? e.getConfidenceScore().doubleValue() : null);
                    map.put("isValidated", e.getValidatedAt() != null);
                    map.put("createdAt", e.getCreatedAt());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(response);
    }
}