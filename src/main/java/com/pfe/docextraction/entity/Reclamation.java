package com.pfe.docextraction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reclamations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reclamation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String sujet;

    @Column(name = "reclamation_type", nullable = true, length = 100)
    private String type;

    @Column(nullable = true, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReclamationPriority priorite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReclamationStatus statut;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dateModification;

    private LocalDateTime dateResolution;

    @Column(name = "piece_jointe", nullable = true, length = 100)
    private String pieceJointe;

    @OneToMany(mappedBy = "reclamation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateMessage ASC")
    @Builder.Default
    private List<ReclamationMessage> messages = new ArrayList<>();
}
