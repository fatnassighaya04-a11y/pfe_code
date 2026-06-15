
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

   
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

   
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

   
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

   
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    
    public boolean isDeleted() {
        return deletedAt != null;
    }

   
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
