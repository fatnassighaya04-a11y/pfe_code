package com.pfe.docextraction.dto;

import com.pfe.docextraction.entity.ReclamationPriority;
import com.pfe.docextraction.entity.ReclamationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReclamationResponse {
    private UUID id;
    private UUID userId;
    private String sujet;
    private String type;
    private String description;
    private ReclamationPriority priorite;
    private ReclamationStatus statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private LocalDateTime dateResolution;
    private String emailUtilisateur;
    private String nomUtilisateur;
    private String roleUtilisateur;
    private String pieceJointe;
    private List<MessageResponse> messages;
}
