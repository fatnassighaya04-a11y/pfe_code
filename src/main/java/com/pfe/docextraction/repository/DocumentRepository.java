
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

   
    Page<Document> findByDeletedAtIsNull(Pageable pageable);

  
    Page<Document> findByUploadedByIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    List<Document> findByStatusAndDeletedAtIsNull(DocumentStatus status);

   
    Page<Document> findByDocumentTypeAndDeletedAtIsNull(DocumentType documentType, Pageable pageable);

  
    Optional<Document> findByChecksumAndDeletedAtIsNull(String checksum);


    long countByStatusAndDeletedAtIsNull(DocumentStatus status);

  
    @Query("SELECT d FROM Document d WHERE d.deletedAt IS NULL " +
           "AND LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Document> searchByFilename(@Param("keyword") String keyword, Pageable pageable);


    @Query("SELECT d.documentType, COUNT(d) FROM Document d " +
           "WHERE d.deletedAt IS NULL GROUP BY d.documentType")
    List<Object[]> countByDocumentType();
}
