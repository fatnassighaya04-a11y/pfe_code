package com.pfe.docextraction.dto;

import com.pfe.docextraction.entity.ReclamationPriority;
import com.pfe.docextraction.entity.ReclamationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReclamationListResponse {
    private UUID id;
    private String sujet;
    private String type;
    private ReclamationPriority priorite;
    private ReclamationStatus statut;
    private LocalDateTime dateCreation;
    private String pieceJointe;
}
