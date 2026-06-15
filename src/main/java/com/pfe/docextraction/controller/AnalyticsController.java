package com.pfe.docextraction.controller;

import com.pfe.docextraction.enums.DocumentStatus;
import com.pfe.docextraction.enums.ExtractionStatus;
import com.pfe.docextraction.enums.AccountStatus;
import com.pfe.docextraction.repository.DocumentRepository;
import com.pfe.docextraction.repository.ExtractionCorrectionRepository;
import com.pfe.docextraction.repository.ExtractionRepository;
import com.pfe.docextraction.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final DocumentRepository documentRepository;
    private final ExtractionRepository extractionRepository;
    private final UserRepository userRepository;
    private final ExtractionCorrectionRepository correctionRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'OPERATEUR')")
    public ResponseEntity<Map<String, Object>> getStats() {

        Map<String, Object> stats = new HashMap<>();

       
        stats.put("totalDocuments",
            documentRepository.countByStatusAndDeletedAtIsNull(DocumentStatus.UPLOADED) +
            documentRepository.countByStatusAndDeletedAtIsNull(DocumentStatus.COMPLETED) +
            documentRepository.countByStatusAndDeletedAtIsNull(DocumentStatus.FAILED) +
            documentRepository.countByStatusAndDeletedAtIsNull(DocumentStatus.PROCESSING));

        stats.put("documentsCompleted",
            documentRepository.countByStatusAndDeletedAtIsNull(DocumentStatus.COMPLETED));

        stats.put("documentsFailed",
            documentRepository.countByStatusAndDeletedAtIsNull(DocumentStatus.FAILED));

        stats.put("documentsProcessing",
            documentRepository.countByStatusAndDeletedAtIsNull(DocumentStatus.PROCESSING));

        stats.put("documentsUploaded",
            documentRepository.countByStatusAndDeletedAtIsNull(DocumentStatus.UPLOADED));

        stats.put("totalExtractions",
            extractionRepository.count());

        stats.put("extractionsSuccess",
            extractionRepository.findByStatusOrderByCreatedAtAsc(
                ExtractionStatus.SUCCESS).size());

        stats.put("extractionsFailed",
            extractionRepository.findByStatusOrderByCreatedAtAsc(
                ExtractionStatus.FAILED).size());

        
        extractionRepository.findAverageConfidenceScore()
            .ifPresent(avg -> stats.put("averageConfidenceScore", avg));

        
        stats.put("totalUsers", userRepository.count());

        stats.put("activeUsers",
            userRepository.findAll().stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
                .count());

        stats.put("pendingUsers",
            userRepository.findAll().stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.PENDING)
                .count());

       
        Map<String, Long> byType = new HashMap<>();
        documentRepository.countByDocumentType().forEach(row -> {
            byType.put(row[0].toString(), (Long) row[1]);
        });
        stats.put("documentsByType", byType);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/extractions-by-day")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'OPERATEUR')")
    public ResponseEntity<Map<String, Object>> getExtractionsByDay() {
        Map<String, Object> response = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("dd/MM");

        Map<LocalDate, Long> countsByDay = extractionRepository.findAll().stream()
            .filter(extraction -> extraction.getCreatedAt() != null)
            .collect(Collectors.groupingBy(
                extraction -> extraction.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            labels.add(day.format(labelFormatter));
            values.add(countsByDay.getOrDefault(day, 0L));
        }

        response.put("labels", labels);
        response.put("data", values);
        response.put("total", extractionRepository.count());

        return ResponseEntity.ok(response);
    }

   
    @GetMapping("/ai-accuracy")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTIONNAIRE', 'OPERATEUR')")
    public ResponseEntity<Map<String, Object>> getAiAccuracy() {
        Map<String, Object> response = new LinkedHashMap<>();

        
        Map<String, Long> correctionsByType = new LinkedHashMap<>();
        correctionRepository.countCorrectionsByType().forEach(row ->
            correctionsByType.put(row[0].toString(), (Long) row[1])
        );
        response.put("correctionsByType", correctionsByType);

        
        Map<String, List<Map<String, Object>>> fieldsByType = new LinkedHashMap<>();
        correctionRepository.countCorrectionsByTypeAndField().forEach(row -> {
            String docType = row[0].toString();
            String fieldName = (String) row[1];
            Long count = (Long) row[2];

            fieldsByType.computeIfAbsent(docType, k -> new ArrayList<>())
                .add(Map.of("field", fieldName, "corrections", count));
        });
        response.put("topFieldsByType", fieldsByType);

        
        response.put("totalCorrections", correctionRepository.count());

       
        long totalExtractions = extractionRepository.count();
        long totalCorrections = correctionRepository.count();
        double accuracyRate = totalExtractions > 0
            ? Math.max(0, 100.0 - (totalCorrections * 100.0 / totalExtractions))
            : 100.0;
        response.put("accuracyRate", Math.round(accuracyRate * 10) / 10.0);

        return ResponseEntity.ok(response);
    }
}