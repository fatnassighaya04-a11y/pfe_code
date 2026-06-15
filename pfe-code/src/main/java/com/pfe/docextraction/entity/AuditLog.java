// ================================================================
// FICHIER : src/main/java/com/pfe/docextraction/entity/AuditLog.java
// RÔLE    : Journal de toutes les actions importantes
//           Qui a fait quoi, quand, depuis quelle IP
//           Obligatoire pour RGPD et traçabilité
// ================================================================
package com.pfe.docextraction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

// Pas d'extension de BaseEntity car AuditLog ne doit JAMAIS être supprimé
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // L'utilisateur qui a effectué l'action (null si non connecté)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Type d'action effectuée
    // Ex: "DOCUMENT_UPLOAD", "USER_LOGIN", "EXTRACTION_VALIDATE", "USER_DELETE"
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    // Type de la ressource concernée
    // Ex: "DOCUMENT", "USER", "EXTRACTION"
    @Column(name = "resource_type", length = 50)
    private String resourceType;

    // ID de la ressource concernée (UUID converti en String)
    @Column(name = "resource_id", length = 36)
    private String resourceId;

    // Adresse IP de l'utilisateur
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // Résultat de l'action : "SUCCESS" ou "FAILURE"
    @Column(name = "result", length = 20)
    private String result;

    // Détails supplémentaires (message d'erreur, info complémentaire)
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    // Date/heure de l'action (automatique)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
