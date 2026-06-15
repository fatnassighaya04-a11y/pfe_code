package com.pfe.docextraction.enums;

public enum ExtractionStatus {
    PENDING,      // créée, pas encore lancée
    IN_PROGRESS,  // extraction en cours
    SUCCESS,      // extraction réussie
    FAILED        // extraction échouée
}