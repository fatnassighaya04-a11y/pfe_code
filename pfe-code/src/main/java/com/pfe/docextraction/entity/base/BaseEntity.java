// ================================================================
// FICHIER : src/main/java/com/pfe/docextraction/entity/base/BaseEntity.java
// RÔLE    : Classe parente de TOUTES les entités
//           Contient les champs communs : id, createdAt, updatedAt
//
// @MappedSuperclass = "cette classe n'est pas une table en BDD,
//                      mais ses champs sont hérités par les entités enfants"
// ================================================================
package com.pfe.docextraction.entity.base;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    // ----------------------------------------------------------------
    // IDENTIFIANT UNIQUE
    // UUID = identifiant universel (ex: "550e8400-e29b-41d4-a716...")
    // Avantages vs auto-increment :
    //   - Impossible à deviner (sécurité)
    //   - Peut être généré côté client
    //   - Pas de conflit lors de merges de BDD
    // ----------------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // ----------------------------------------------------------------
    // DATE DE CRÉATION
    // @CreationTimestamp = Hibernate remplit automatiquement à l'INSERT
    // updatable = false = ne peut plus être modifié après création
    // ----------------------------------------------------------------
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ----------------------------------------------------------------
    // DATE DE DERNIÈRE MODIFICATION
    // @UpdateTimestamp = Hibernate met à jour automatiquement à chaque UPDATE
    // ----------------------------------------------------------------
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ----------------------------------------------------------------
    // SOFT DELETE — Date de suppression logique
    // null = document actif
    // non-null = document "supprimé" (mais toujours en BDD pour l'historique)
    // ----------------------------------------------------------------
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Méthode utilitaire : vérifie si l'entité est supprimée
    public boolean isDeleted() {
        return deletedAt != null;
    }

    // Méthode utilitaire : effectue le soft delete
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
