package com.pfe.docextraction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reclamation_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReclamationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reclamation_id", nullable = false)
    private Reclamation reclamation;

    @Column(nullable = false)
    private String auteur;

    @Column(nullable = false, length = 5000)
    private String contenu;

    private boolean estReponseAdmin;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateMessage;
}
