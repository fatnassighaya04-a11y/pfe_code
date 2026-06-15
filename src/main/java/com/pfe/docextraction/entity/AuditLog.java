
package com.pfe.docextraction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;


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

 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    
    @Column(name = "action", nullable = false, length = 100)
    private String action;

   
    @Column(name = "resource_type", length = 50)
    private String resourceType;

   
    @Column(name = "resource_id", length = 36)
    private String resourceId;

    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    
    @Column(name = "result", length = 20)
    private String result;

    
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

   
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
