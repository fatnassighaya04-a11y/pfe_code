package com.pfe.docextraction.repository;

import com.pfe.docextraction.entity.Extraction;
import com.pfe.docextraction.enums.ExtractionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExtractionRepository extends JpaRepository<Extraction, UUID> {

    Optional<Extraction> findTopByDocumentIdAndStatusOrderByCreatedAtDesc(
        UUID documentId, ExtractionStatus status
    );

    List<Extraction> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);

    List<Extraction> findByStatusOrderByCreatedAtAsc(ExtractionStatus status);

    @Query("SELECT e FROM Extraction e WHERE e.confidenceScore < :threshold " +
           "AND e.status = 'SUCCESS' AND e.validatedAt IS NULL")
    List<Extraction> findLowConfidenceUnvalidated(@Param("threshold") BigDecimal threshold);

    @Query("SELECT AVG(e.confidenceScore) FROM Extraction e WHERE e.status = 'SUCCESS'")
    Optional<BigDecimal> findAverageConfidenceScore();
}