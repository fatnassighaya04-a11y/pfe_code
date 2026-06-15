package com.pfe.docextraction.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MessageResponse {
    private UUID id;
    private String auteur;
    private String contenu;
    private boolean estReponseAdmin;
    private LocalDateTime dateMessage;
}
