// ================================================================
// FICHIER : src/main/java/com/pfe/docextraction/entity/Document.java
// RÔLE    : Entité représentant un document uploadé
//           Correspond à la table "documents" en PostgreSQL
// ================================================================
package com.pfe.docextraction.entity;

import com.pfe.docextraction.entity.base.BaseEntity;
import com.pfe.docextraction.enums.DocumentStatus;
import com.pfe.docextraction.enums.DocumentType;
import com.pfe.docextraction.enums.FileType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "documents", indexes = {
    // Index sur uploaded_by pour accélérer "mes documents"
    @Index(name = "idx_documents_uploaded_by", columnList = "uploaded_by"),
    // Index sur status pour accélérer le filtrage par statut
    @Index(name = "idx_documents_status", columnList = "status"),
    // Index sur deleted_at pour le soft delete
    @Index(name = "idx_documents_deleted_at", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends BaseEntity {

    // ----------------------------------------------------------------
    // INFORMATIONS DU FICHIER
    // ----------------------------------------------------------------

    // Nom original tel que l'utilisateur l'a uploadé (ex: "facture_avril.pdf")
    // Utilisé uniquement pour l'affichage, jamais pour le chemin de stockage !
    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    // Nom stocké sur disque : toujours un UUID pour éviter les conflits
    // et empêcher la traversée de répertoires (path traversal attack)
    // Ex: "550e8400-e29b-41d4-a716-446655440000.pdf"
    @Column(name = "stored_filename", nullable = false, length = 500)
    private String storedFilename;

    // Chemin complet sur le système de fichiers
    // Ex: "/data/uploads/2024/01/550e8400...pdf"
    @Column(name = "file_path", nullable = false)
    private String filePath;

    // Type de fichier (enum FileType)
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    // Taille en octets (pour afficher "2.3 MB" à l'utilisateur)
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    // Type MIME réel (vérifié par Apache Tika, pas juste l'extension)
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    // Hash SHA-256 du contenu : permet de détecter les doublons
    // Si deux fichiers ont le même checksum → fichier identique
    @Column(name = "checksum", length = 64)
    private String checksum;

    // ----------------------------------------------------------------
    // STATUT ET TYPE
    // ----------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    @Builder.Default
    private DocumentType documentType = DocumentType.AUTRE;

    // ----------------------------------------------------------------
    // RELATION VERS L'UTILISATEUR QUI A UPLOADÉ
    // @ManyToOne = plusieurs documents → un seul utilisateur
    // @JoinColumn = nom de la colonne FK dans la table "documents"
    // LAZY = ne charge pas l'utilisateur automatiquement (performance)
    // ----------------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    // ----------------------------------------------------------------
    // RELATION VERS LES EXTRACTIONS
    // Un document peut avoir plusieurs tentatives d'extraction
    // ----------------------------------------------------------------
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Extraction> extractions;

    // ----------------------------------------------------------------
    // MÉTHODES UTILITAIRES
    // ----------------------------------------------------------------

    // Retourne la taille formatée (ex: "2.3 MB")
    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }
}
