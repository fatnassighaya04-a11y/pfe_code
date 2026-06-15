package com.pfe.docextraction.entity;

import com.pfe.docextraction.entity.base.BaseEntity;
import com.pfe.docextraction.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "extraction_corrections", indexes = {
    @Index(name = "idx_corrections_field", columnList = "field_name"),
    @Index(name = "idx_corrections_doc_type", columnList = "document_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractionCorrection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_id", nullable = false)
    private Extraction extraction;


    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;


    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "ai_value", columnDefinition = "TEXT")
    private String aiValue;

   
    @Column(name = "human_value", columnDefinition = "TEXT")
    private String humanValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_by")
    private User correctedBy;
}
