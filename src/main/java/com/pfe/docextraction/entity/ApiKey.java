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

    
    @Column(name = "key_value", nullable = false, unique = true, length = 64)
    private String keyValue;

    
    @Column(name = "app_name", nullable = false, length = 100)
    private String appName;

    
    @Column(name = "description", length = 255)
    private String description;

   
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

   
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

   
    public boolean isValid() {
        if (!isActive) return false;
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) return false;
        return true;
    }
}