package com.pfe.docextraction.repository;

import com.pfe.docextraction.entity.ExtractionCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExtractionCorrectionRepository extends JpaRepository<ExtractionCorrection, UUID> {

    /**
     * Stats agrégées par type de document et champ : combien de fois chaque champ a été corrigé.
     * Renvoie [documentType, fieldName, correctionCount].
     */
    @Query("""
        SELECT c.documentType, c.fieldName, COUNT(c)
        FROM ExtractionCorrection c
        WHERE c.deletedAt IS NULL
        GROUP BY c.documentType, c.fieldName
        ORDER BY COUNT(c) DESC
    """)
    List<Object[]> countCorrectionsByTypeAndField();

    /** Total de corrections par type de document */
    @Query("""
        SELECT c.documentType, COUNT(c)
        FROM ExtractionCorrection c
        WHERE c.deletedAt IS NULL
        GROUP BY c.documentType
    """)
    List<Object[]> countCorrectionsByType();

    /** Récupère les corrections d'une extraction spécifique */
    List<ExtractionCorrection> findByExtractionIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID extractionId);
}
