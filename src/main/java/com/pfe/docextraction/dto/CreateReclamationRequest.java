package com.pfe.docextraction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReclamationRequest {
    @NotBlank(message = "Le type est obligatoire")
    private String type;

    @NotBlank(message = "Le sujet est obligatoire")
    @Size(min = 5, max = 200)
    private String sujet;

    private String description;

    // Optional: id or stored filename of uploaded document
    private String pieceJointe;

    private String priorite;
}
