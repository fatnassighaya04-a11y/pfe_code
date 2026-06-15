
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


    @Column(name = "token", nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

 
    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private Boolean isRevoked = false;

   
    @Column(name = "created_from_ip", length = 45)  
    private String createdFromIp;

    public boolean isValid() {
        return !isRevoked && LocalDateTime.now().isBefore(expiresAt);
    }
}
