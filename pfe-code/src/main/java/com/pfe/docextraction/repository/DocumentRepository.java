// ================================================================
// FICHIER : DocumentRepository.java
// ================================================================
package com.pfe.docextraction.repository;

import com.pfe.docextraction.entity.Document;
import com.pfe.docextraction.enums.DocumentStatus;
import com.pfe.docextraction.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Tous les documents non supprimés, paginés
    // Page<Document> = résultat paginé (page 1, 20 éléments, etc.)
    // Pageable = paramètre de pagination passé par le controller
    Page<Document> findByDeletedAtIsNull(Pageable pageable);

    // Documents d'un utilisateur spécifique (non supprimés)
    Page<Document> findByUploadedByIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    // Documents par statut
    List<Document> findByStatusAndDeletedAtIsNull(DocumentStatus status);

    // Documents par type
    Page<Document> findByDocumentTypeAndDeletedAtIsNull(DocumentType documentType, Pageable pageable);

    // Trouver par checksum (détecter les doublons)
    Optional<Document> findByChecksumAndDeletedAtIsNull(String checksum);

    // Compter les documents par statut (pour le dashboard)
    long countByStatusAndDeletedAtIsNull(DocumentStatus status);

    // Recherche par nom de fichier (pour la barre de recherche)
    @Query("SELECT d FROM Document d WHERE d.deletedAt IS NULL " +
           "AND LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Document> searchByFilename(@Param("keyword") String keyword, Pageable pageable);

    // Statistiques : nombre de documents par type
    @Query("SELECT d.documentType, COUNT(d) FROM Document d " +
           "WHERE d.deletedAt IS NULL GROUP BY d.documentType")
    List<Object[]> countByDocumentType();
}
