package com.pfe.docextraction.dto;

import com.pfe.docextraction.entity.ReclamationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    @NotNull
    private ReclamationStatus statut;
    private String commentaire;
}
