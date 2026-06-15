package com.pfe.docextraction.entity;

import com.pfe.docextraction.entity.base.BaseEntity;
import com.pfe.docextraction.enums.ExtractionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "extractions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Extraction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "extracted_data", columnDefinition = "TEXT")
    private String extractedData;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "extraction_model", length = 100)
    private String extractionModel;

    @Column(name = "extraction_duration_ms")
    private Integer extractionDurationMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ExtractionStatus status = ExtractionStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "validation_notes", columnDefinition = "TEXT")
    private String validationNotes;

    // ✅ NOUVEAU CHAMP : seuil d'auto-validation utilisé
    @Column(name = "auto_validation_threshold")
    private Double autoValidationThreshold;
}