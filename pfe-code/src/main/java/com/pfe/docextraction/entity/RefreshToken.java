// ================================================================
// FICHIER : src/main/java/com/pfe/docextraction/entity/RefreshToken.java
// RÔLE    : Stocke les refresh tokens JWT en base de données
//           Permet de les invalider (logout, rotation de tokens)
// ================================================================
package com.pfe.docextraction.entity;

import com.pfe.docextraction.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_user", columnList = "user_id"),
    @Index(name = "idx_refresh_tokens_token", columnList = "token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    // Le token lui-même (longue chaîne aléatoire)
    // unique = un seul enregistrement par token
    @Column(name = "token", nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    // À qui appartient ce token
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Quand ce token expire (7 jours après création)
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Token révoqué manuellement (logout ou rotation)
    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private Boolean isRevoked = false;

    // Adresse IP qui a créé ce token (pour sécurité)
    @Column(name = "created_from_ip", length = 45)  // 45 chars pour IPv6
    private String createdFromIp;

    // Méthode utilitaire : est-ce que ce token est encore valide ?
    public boolean isValid() {
        return !isRevoked && LocalDateTime.now().isBefore(expiresAt);
    }
}
