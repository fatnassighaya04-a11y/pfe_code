package com.pfe.docextraction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // La clé API générée
    @Column(name = "key_value", nullable = false, unique = true, length = 64)
    private String keyValue;

    // Nom de l'application qui utilise cette clé
    // Ex: "ERP SAP", "Logiciel Comptable"
    @Column(name = "app_name", nullable = false, length = 100)
    private String appName;

    // Description de l'utilisation
    @Column(name = "description", length = 255)
    private String description;

    // Qui a généré cette clé
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // Clé active ou révoquée
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Date d'expiration (null = pas d'expiration)
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Dernière utilisation
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Méthode utilitaire : clé valide ?
    public boolean isValid() {
        if (!isActive) return false;
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) return false;
        return true;
    }
}